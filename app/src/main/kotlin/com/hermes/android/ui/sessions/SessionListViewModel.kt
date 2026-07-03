package com.hermes.android.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.models.SessionSummary
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.HermesApp
import com.hermes.android.data.auth.AuthStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionListViewModel : ViewModel() {
    private val authStore = AuthStore(HermesApp.instance)
    private val apiClient: ApiClient? by lazy {
        val url = authStore.serverUrl
        if (url != null) ApiClient(url, HermesApp.instance.httpClient) else null
    }

    private val _state = MutableStateFlow(SessionListState())
    val state: StateFlow<SessionListState> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init {
        loadSessions()
    }

    private fun loadSessions() {
        val client = apiClient ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = client.getSessions()
                _state.update { it.copy(sessions = response.sessions, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Unknown error", isLoading = false) }
            }
        }
    }

    fun refresh() {
        val client = apiClient ?: return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                val response = client.getSessions()
                _state.update { it.copy(sessions = response.sessions, error = null) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Unknown error") }
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun newSession(onNewSession: (String) -> Unit) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                val session = client.newSession()
                onNewSession(session.sessionId)
                loadSessions()
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                client.deleteSession(sessionId)
                loadSessions()
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                client.renameSession(sessionId, newTitle)
                loadSessions()
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }

    fun pinSession(sessionId: String, pin: Boolean) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                client.pinSession(sessionId, pin)
                loadSessions()
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }

    fun archiveSession(sessionId: String, archive: Boolean) {
        val client = apiClient ?: return
        viewModelScope.launch {
            try {
                client.archiveSession(sessionId, archive)
                loadSessions()
            } catch (e: Exception) {
                // TODO: handle error
            }
        }
    }
}

data class SessionListState(
    val sessions: List<SessionSummary> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
)