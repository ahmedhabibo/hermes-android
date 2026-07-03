package com.hermes.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.models.ChatMessage
import com.hermes.android.data.models.ModelInfo
import com.hermes.android.data.models.ProviderOption
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.ApiError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.*

data class ChatUiState(
    val sessionId: String = "",
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val models: List<ModelInfo> = emptyList(),
    val selectedModel: String? = null,
    val modelName: String = "",
    val error: String? = null
)

class ChatViewModel(
    private val apiClient: ApiClient,
    private val sessionId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(sessionId = sessionId))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val resp = apiClient.getSessionMessages(sessionId)
                _uiState.update { it.copy(
                    messages = resp.messages,
                    isLoading = false
                )}
            } catch (e: ApiError) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadModels() {
        viewModelScope.launch {
            try {
                val resp = apiClient.getModelOptions()
                // Flatten all models from all providers
                val allModels = resp.providers.flatMap { provider ->
                    provider.models.map { it.copy(name = "${provider.label}: ${it.name}") }
                }
                _uiState.update { it.copy(models = allModels) }

                // Set default model
                try {
                    val default = apiClient.getDefaultModel()
                    if (default.model.isNotBlank()) {
                        _uiState.update { it.copy(
                            selectedModel = default.model,
                            modelName = "${default.provider}: ${default.model}"
                        )}
                    }
                } catch (_: Exception) { /* default model optional */ }
            } catch (e: Exception) {
                // Models are optional — don't block chat if they fail
            }
        }
    }

    fun selectModel(modelId: String) {
        _uiState.update { state ->
            state.copy(
                selectedModel = modelId,
                modelName = state.models.firstOrNull { it.id == modelId }?.name ?: modelId
            )
        }
    }

    fun sendMessage(text: String, attachmentUris: List<String>) {
        if (text.isBlank() && attachmentUris.isEmpty()) return

        val userMsg = ChatMessage(
            role = "user",
            content = text,
            timestamp = System.currentTimeMillis() / 1000.0,
            attachments = attachmentUris.map { uri ->
                com.hermes.android.data.models.MessageAttachment(
                    name = uri.substringAfterLast("/"),
                    path = uri,
                    isImage = uri.startsWith("content://") || uri.startsWith("http")
                )
            }
        )

        // Add user message immediately
        _uiState.update { it.copy(
            messages = it.messages + userMsg,
            isStreaming = true
        )}

        // TODO: Send via WebSocket /api/pty
        // For now, show a placeholder assistant response until WebSocket is wired
        viewModelScope.launch {
            _uiState.update { it.copy(isStreaming = false) }
            val placeholder = ChatMessage(
                role = "assistant",
                content = "WebSocket chat not yet connected. Message received: \"$text\"",
                timestamp = System.currentTimeMillis() / 1000.0
            )
            _uiState.update { it.copy(messages = it.messages + placeholder) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // WebSocket cleanup will go here once /api/pty is wired
    }
}
