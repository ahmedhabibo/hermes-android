package com.hermes.android.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.data.models.SessionMutationResponse
import com.hermes.android.data.models.SessionsResponse
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.ApiError
import com.hermes.android.HermesApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionListState(
    val sessions: List<com.hermes.android.data.models.SessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionListViewModel : ViewModel() {
    private val _state = MutableStateFlow(SessionListState(isLoading = true))
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    private val authStore = AuthStore(HermesApp.instance)
    private val apiClient: ApiClient? by lazy {
        val url = authStore.serverUrl
        if (url != null) ApiClient(url, HermesApp.instance.httpClient) else null
    }

    init {
        refresh()
    }

    fun refresh() {
        val client = apiClient ?: run {
            _state.update { it.copy(isLoading = false, error = "Not configured — please reconnect") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val resp: SessionsResponse = client.getSessions()
                val sessions = resp.sessions.sortedByDescending { it.updatedAt }
                _state.update { it.copy(sessions = sessions, isLoading = false) }
            } catch (e: ApiError.Http) {
                _state.update { it.copy(isLoading = false, error = "HTTP ${e.statusCode}") }
            } catch (e: ApiError.Network) {
                _state.update { it.copy(isLoading = false, error = "Network error: ${e.underlying.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }

    fun newSession(onCreated: (String) -> Unit) {
        val client = apiClient ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val resp: SessionMutationResponse = client.newSession()
                if (resp.ok && resp.sessionId.isNotEmpty()) {
                    _state.update { it.copy(isLoading = false) }
                    onCreated(resp.sessionId)
                } else {
                    _state.update { it.copy(isLoading = false, error = resp.error.ifBlank { "Failed to create session" }) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Error: ${e.message}") }
            }
        }
    }
}