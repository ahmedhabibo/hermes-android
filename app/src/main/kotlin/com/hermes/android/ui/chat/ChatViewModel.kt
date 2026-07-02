package com.hermes.android.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.HermesApp
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.data.models.ChatStartResponse
import com.hermes.android.data.models.SSEEvent
import com.hermes.android.data.models.SessionDetail
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.ApiError
import com.hermes.android.data.networking.SSEClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream

data class ChatMessage(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val isStreaming: Boolean = false,
    val attachments: List<String> = emptyList()
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val sessionTitle: String = "",
    val selectedModel: String = "",
    val pendingAttachments: List<Uri> = emptyList()
)

class ChatViewModel : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val authStore = AuthStore(HermesApp.instance)
    private val apiClient: ApiClient? by lazy {
        val url = authStore.serverUrl
        if (url != null) ApiClient(url, HermesApp.instance.httpClient) else null
    }
    private var sseClient: SSEClient? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    fun loadSession(sessionId: String) {
            val client = apiClient ?: return
            viewModelScope.launch {
                try {
                    val detail = client.getSession(sessionId, includeMessages = true, messageLimit = 50)
                    _state.update { it.copy(sessionTitle = detail.title.ifBlank { "Chat" }) }

                    // Parse messages from JSON strings
                    val messages = detail.messages.mapNotNull { msgJson ->
                        try {
                            if (msgJson.isBlank()) return@mapNotNull null
                            val parsed = json.decodeFromString<com.hermes.android.data.models.ChatMessage>(msgJson)
                            val messageId = if (parsed.messageId.isNotBlank()) parsed.messageId else {
                                val role = parsed.role.ifBlank { "unknown" }
                                val timestamp = if (parsed.timestamp > 0L) parsed.timestamp.toLong() else System.currentTimeMillis()
                                "$role-$timestamp"
                            }
                            ChatMessage(
                                id = messageId,
                                role = parsed.role,
                                content = parsed.content,
                                isStreaming = false,
                                attachments = parsed.attachments
                            )
                        } catch (e: Exception) {
                            // If it's not a valid JSON, treat as a plain text message from an unknown role
                            if (msgJson.isNotBlank()) {
                                ChatMessage(
                                    id = "msg-${System.currentTimeMillis()}",
                                    role = "unknown",
                                    content = msgJson,
                                    isStreaming = false,
                                    attachments = emptyList()
                                )
                            } else null
                        }
                    }
                    _state.update { it.copy(messages = messages) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = "Failed to load session: ${e.message}") }
                }
            }
        }

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun updateModel(model: String) {
        _state.update { it.copy(selectedModel = model) }
    }

    fun addAttachment(uri: Uri) {
        _state.update { it.copy(pendingAttachments = it.pendingAttachments + uri) }
    }

    fun removeAttachment(uri: Uri) {
        _state.update { it.copy(pendingAttachments = it.pendingAttachments - uri) }
    }

    private suspend fun uriToTempFile(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            val displayName = cursor?.use {
                val nameIdx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                it.moveToFirst()
                if (nameIdx >= 0) it.getString(nameIdx) else "upload_${System.currentTimeMillis()}"
            } ?: "upload_${System.currentTimeMillis()}"
            cursor?.close()

            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val ext = when {
                mimeType.contains("jpeg") || mimeType.contains("jpg") -> ".jpg"
                mimeType.contains("png") -> ".png"
                mimeType.contains("pdf") -> ".pdf"
                else -> ""
            }
            val tempFile = File(context.cacheDir, "$displayName$ext")
            FileOutputStream(tempFile).use { out -> inputStream.copyTo(out) }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    fun sendMessage(sessionId: String) {
        val client = apiClient ?: return
        val message = _state.value.inputText.trim()
        if (message.isEmpty() && _state.value.pendingAttachments.isEmpty()) return
        if (_state.value.isSending) return

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isSending = true,
                    isStreaming = true,
                    inputText = "",
                    error = null,
                    messages = it.messages + ChatMessage(
                        id = "user-${System.currentTimeMillis()}",
                        role = "user",
                        content = message,
                        attachments = _state.value.pendingAttachments.map { it.toString() }
                    )
                )
            }

            try {
                // Upload attachments first
                val uploadedPaths = mutableListOf<String>()
                for (uri in _state.value.pendingAttachments) {
                    val file = uriToTempFile(HermesApp.instance, uri)
                    if (file != null) {
                        try {
                            val mimeType = HermesApp.instance.contentResolver.getType(uri) ?: "application/octet-stream"
                            val resp = client.uploadFile(sessionId, file, mimeType)
                            if (resp.path.isNotEmpty()) uploadedPaths.add(resp.path)
                        } catch (e: Exception) {
                            // Skip failed uploads
                        }
                    }
                }
                _state.update { it.copy(pendingAttachments = emptyList()) }

                // Start chat with uploaded attachments
                val startResp: ChatStartResponse = client.chatStart(
                    sessionId = sessionId,
                    message = message,
                    model = _state.value.selectedModel.ifBlank { null },
                    attachments = uploadedPaths.ifEmpty { null }
                )

                if (startResp.streamId.isNotBlank()) {
                    val streamUrl = client.streamUrl(startResp.streamId)
                    val sse = SSEClient(HermesApp.instance.httpClient)
                    sseClient = sse

                    val assistantMsgId = "assistant-${System.currentTimeMillis()}"
                    _state.update {
                        it.copy(
                            messages = it.messages + ChatMessage(
                                id = assistantMsgId,
                                role = "assistant",
                                content = "",
                                isStreaming = true
                            )
                        )
                    }

                    sse.connect(streamUrl).collect { event ->
                        when (event) {
                            is SSEEvent.Token -> {
                                _state.update { state ->
                                    val msgs = state.messages.map {
                                        if (it.id == assistantMsgId) it.copy(content = it.content + event.text)
                                        else it
                                    }
                                    state.copy(messages = msgs)
                                }
                            }
                            is SSEEvent.Reasoning -> {
                                _state.update { state ->
                                    val msgs = state.messages.map {
                                        if (it.id == assistantMsgId) it.copy(content = it.content + "💭 ${event.text}\n")
                                        else it
                                    }
                                    state.copy(messages = msgs)
                                }
                            }
                            is SSEEvent.ToolStarted -> {
                                _state.update { state ->
                                    state.copy(
                                        messages = state.messages + ChatMessage(
                                            id = "tool-start-${System.currentTimeMillis()}",
                                            role = "tool",
                                            content = "🔧 ${event.name ?: "Tool"}: ${event.preview ?: ""}"
                                        )
                                    )
                                }
                            }
                            is SSEEvent.ToolCompleted -> {
                                val toolMsgId = "tool-done-${System.currentTimeMillis()}"
                                _state.update { state ->
                                    state.copy(
                                        messages = state.messages + ChatMessage(
                                            id = toolMsgId,
                                            role = "tool",
                                            content = "✅ ${event.name ?: "Tool"} completed: ${event.preview ?: ""} ${if (event.isError == true) "❌" else ""}"
                                        )
                                    )
                                }
                            }
                            is SSEEvent.Title -> {
                                _state.update { it.copy(sessionTitle = event.title ?: it.sessionTitle) }
                            }
                            is SSEEvent.Done, is SSEEvent.StreamEnd -> {
                                _state.update { state ->
                                    val msgs = state.messages.map {
                                        if (it.id == assistantMsgId) it.copy(isStreaming = false) else it
                                    }
                                    state.copy(messages = msgs, isSending = false, isStreaming = false)
                                }
                            }
                            is SSEEvent.Cancelled -> {
                                _state.update { state ->
                                    val msgs = state.messages.map {
                                        if (it.id == assistantMsgId) it.copy(isStreaming = false, content = it.content + "\n[cancelled]") else it
                                    }
                                    state.copy(messages = msgs, isSending = false, isStreaming = false)
                                }
                            }
                            is SSEEvent.Error -> {
                                _state.update { state ->
                                    val msgs = state.messages.map {
                                        if (it.id == assistantMsgId) it.copy(isStreaming = false, content = it.content + "\n[error: ${event.message}]") else it
                                    }
                                    state.copy(messages = msgs, isSending = false, isStreaming = false, error = event.message)
                                }
                            }
                            is SSEEvent.TransportError -> {
                                _state.update {
                                    it.copy(isSending = false, isStreaming = false, error = event.message)
                                }
                            }
                            is SSEEvent.InterimAssistant -> {
                                // Refers to already-streamed content — no action needed
                            }
                            is SSEEvent.Ignored -> {}
                        }
                    }
                } else {
                    _state.update { it.copy(isSending = false, isStreaming = false, error = "No stream ID received") }
                }
            } catch (e: ApiError.Http) {
                _state.update { it.copy(isSending = false, isStreaming = false, error = "HTTP ${e.statusCode}: ${e.body?.take(100)}") }
            } catch (e: ApiError.Network) {
                _state.update { it.copy(isSending = false, isStreaming = false, error = "Network: ${e.underlying.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(isSending = false, isStreaming = false, error = "Error: ${e.message}") }
            }
        }
    }

    fun cancelStream() {
        sseClient?.disconnect()
        sseClient = null
        _state.update {
            it.copy(
                isSending = false,
                isStreaming = false,
                messages = it.messages.map { msg -> if (msg.isStreaming) msg.copy(isStreaming = false) else msg }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sseClient?.disconnect()
    }
}