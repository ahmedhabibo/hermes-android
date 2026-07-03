package com.hermes.android.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.models.SessionSummary
import com.hermes.android.data.models.SessionsResponse
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionListUiState(
    val sessions: List<SessionSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionListViewModel(private val apiClient: ApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionListUiState())
    val uiState: StateFlow<SessionListUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = apiClient.getSessions()
                _uiState.update { it.copy(sessions = response.sessions, isLoading = false) }
            } catch (e: ApiError) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Unknown error") }
            }
        }
    }

    fun newSession(workspace: String? = null, model: String? = null, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val resp = apiClient.newSession(workspace, model)
                if (resp.ok && resp.sessionId.isNotBlank()) {
                    refresh()
                    onCreated(resp.sessionId)
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Failed to create session") }
                }
            } catch (e: ApiError) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                apiClient.renameSession(sessionId, newTitle)
                _uiState.update { state ->
                    state.copy(sessions = state.sessions.map {
                        if (it.sessionId == sessionId) it.copy(title = newTitle) else it
                    })
                }
            } catch (e: ApiError) {
                _uiState.update { it.copy(error = "Rename failed: ${e.message}") }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                apiClient.deleteSession(sessionId)
                _uiState.update { state ->
                    state.copy(sessions = state.sessions.filter { it.sessionId != sessionId })
                }
            } catch (e: ApiError) {
                _uiState.update { it.copy(error = "Delete failed: ${e.message}") }
            }
        }
    }

    fun archiveSession(sessionId: String, archive: Boolean) {
        viewModelScope.launch {
            try {
                apiClient.archiveSession(sessionId, archive)
                _uiState.update { state ->
                    state.copy(sessions = state.sessions.map {
                        if (it.sessionId == sessionId) it.copy(archived = archive) else it
                    })
                }
            } catch (e: ApiError) {
                _uiState.update { it.copy(error = "Archive failed: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
