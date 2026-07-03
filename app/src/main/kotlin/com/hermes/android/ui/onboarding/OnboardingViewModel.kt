package com.hermes.android.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.HermesApp
import com.hermes.android.data.networking.ApiClient
import com.hermes.android.data.networking.ApiError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(
    val serverUrl: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val urlError: String? = null,
    val isLoggedIn: Boolean = false
)

class OnboardingViewModel : ViewModel() {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    private val authStore = AuthStore(HermesApp.instance)

    init {
        authStore.serverUrl?.let { savedUrl ->
            _state.update { state -> state.copy(serverUrl = savedUrl) }
        }
        if (authStore.isLoggedIn && authStore.serverUrl != null) {
            _state.update { it.copy(isLoggedIn = true) }
        }
    }

    fun updateServerUrl(url: String) {
        _state.update { it.copy(serverUrl = url, urlError = null, error = null) }
    }

    fun updatePassword(pwd: String) {
        _state.update { it.copy(password = pwd, error = null) }
    }

    fun connect(onSuccess: () -> Unit) {
        val url = state.value.serverUrl.trim().removeSuffix("/")
        if (url.isBlank()) {
            _state.update { it.copy(urlError = "Server URL is required") }
            return
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _state.update { it.copy(urlError = "URL must start with http:// or https://") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val client = ApiClient(url, HermesApp.instance.httpClient)

            try {
                val password = state.value.password
                if (password.isNotEmpty()) {
                    // POST /auth/password-login
                    client.passwordLogin("admin", password)
                }

                // Verify auth with GET /api/auth/me
                try {
                    client.authMe()
                } catch (e: ApiError.Unauthorized) {
                    _state.update { it.copy(isLoading = false, error = "Login failed — check password") }
                    return@launch
                }

                authStore.serverUrl = url
                if (password.isNotEmpty()) {
                    authStore.password = password
                }
                authStore.isLoggedIn = true

                _state.update { it.copy(isLoading = false) }
                onSuccess()

            } catch (e: ApiError.Http) {
                val msg = when (e.statusCode) {
                    401 -> "Authentication required — please enter the server password"
                    403 -> "Invalid password"
                    404 -> "Server not found at $url — check the URL and port"
                    else -> "Server error: HTTP ${e.statusCode}"
                }
                _state.update { it.copy(isLoading = false, error = msg) }
            } catch (e: ApiError.Network) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Cannot reach server at $url — ${e.underlying.message ?: "check network and server"}"
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Unexpected error: ${e.message ?: "unknown"}"
                    )
                }
            }
        }
    }
}
