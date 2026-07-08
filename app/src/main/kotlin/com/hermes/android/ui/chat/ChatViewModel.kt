package com.hermes.android.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.models.ChatMessage
import com.hermes.android.data.networking.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ChatUiState(
    val sessionId: String = "",
    val title: String = "Chat",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val apiClient: ApiClient,
    private val sessionId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState(
        sessionId = sessionId,
        title = "Loading..."
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Try to get session title from sessions list (fallback to placeholder)
                val sessionsResp = apiClient.getSessions()
                val session = sessionsResp.sessions.firstOrNull { it.sessionId == sessionId }
                val title = session?.title ?: "Chat Session $sessionId"
                _uiState.update { it.copy(title = title, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Add user message
        val userMsg = ChatMessage(
            role = "user",
            content = text,
            timestamp = System.currentTimeMillis() / 1000.0,
            messageId = java.util.UUID.randomUUID().toString()
        )

        _uiState.update { it.copy(
            messages = it.messages + userMsg
        )}

        // Simulate bot response (placeholder)
        viewModelScope.launch {
            delay(800) // Simulate thinking time
            val botMsg = ChatMessage(
                role = "assistant",
                content = "You said: \"$text\"", // Simple echo for demo
                timestamp = System.currentTimeMillis() / 1000.0,
                messageId = java.util.UUID.randomUUID().toString()
            )
            _uiState.update { it.copy(
                messages = it.messages + botMsg
            )}
        }
    }
}