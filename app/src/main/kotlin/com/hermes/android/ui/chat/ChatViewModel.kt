package com.hermes.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.data.models.SSEEvent
import com.hermes.android.data.models.ChatStartResponse
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.SSEClient
import com.hermes.android.data.networking.ApiError
import com.hermes.android.HermesApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = "",
    val role: String = "",
    val content: String = "",
    val isStreaming: Boolean = false
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val sessionTitle: String = ""
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

    fun loadSession(sessionId: String) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                val detail = client.getSession(sessionId, includeMessages = true, messageLimit = 50)
                _state.update { it.copy(sessionTitle = detail.title.ifBlank { "Chat" }) }
                // Would parse messages here if server returns them as structured data
            } catch (e: Exception) {
                // Silent fail on load
            }
        }
    }

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun sendMessage(sessionId: String) {
        val client = apiClient ?: return
        val message = _state.value.inputText.trim()
        if (message.isEmpty() || _state.value.isSending) return

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
                        content = message
                    )
                )
            }

            try {
                // Start chat
                val startResp: ChatStartResponse = client.chatStart(
                    sessionId = sessionId,
                    message = message
                )

                if (startResp.streamId.isNotBlank()) {
                    // Connect SSE stream
                    val streamUrl = client.streamUrl(startResp.streamId)
                    val sse = SSEClient(HermesApp.instance.httpClient)
                    sseClient = sse

                    // Add placeholder assistant message for streaming
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
                                // Could show reasoning in a separate area
                            }
                            is SSEEvent.ToolStarted -> {
                                val toolMsgId = "tool-${System.currentTimeMillis()}"
                                _state.update { state ->
                                    state.copy(
                                        messages = state.messages + ChatMessage(
                                            id = toolMsgId,
                                            role = "tool",
                                            content = "🔧 ${event.name ?: "Tool"}: ${event.preview ?: ""}"
                                        )
                                    )
                                }
                            }
                            is SSEEvent.ToolCompleted -> {
                                // Could update tool message with results
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
                                // Refers to already-streamed content
                            }
                            is SSEEvent.Ignored -> {}
                        }
                    }
                } else {
                    _state.update { it.copy(isSending = false, isStreaming = false, error = "No stream ID received") }
                }
            } catch (e: ApiError.Http) {
                _state.update { it.copy(isSending = false, isStreaming = false, error = "HTTP ${e.statusCode}") }
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