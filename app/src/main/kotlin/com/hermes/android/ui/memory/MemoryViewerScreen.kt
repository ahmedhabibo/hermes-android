package com.hermes.android.ui.memory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.HermesApp
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.data.models.MemoryResponse
import com.hermes.android.data.networking.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemoryViewModel : ViewModel() {
    private val authStore = AuthStore(HermesApp.instance)

    private val _apiClient: ApiClient? by lazy {
        val url = authStore.serverUrl
        if (url != null) ApiClient(url, HermesApp.instance.httpClient) else null
    }


    private val _memory = MutableStateFlow<MemoryResponse?>(null)
    val memory: StateFlow<MemoryResponse?> = _memory.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init { load() }

    fun load() {
        val client = _apiClient
        if (client == null) {
            _error.value = "Not configured"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _memory.value = client.getMemory()
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load memory"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryViewerScreen(
    onBack: () -> Unit,
    viewModel: MemoryViewModel = viewModel()
) {
    val memory by viewModel.memory.collectAsState()
    val error by viewModel.error.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Memory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                error != null -> Text(text = error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center).padding(16.dp))
                memory != null -> {
                    val mem = memory!!
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (mem.userProfile.isNotBlank()) {
                            Text("User Profile", style = MaterialTheme.typography.titleMedium)
                            Text(mem.userProfile, style = MaterialTheme.typography.bodyMedium)
                            HorizontalDivider()
                        }
                        if (mem.notes.isNotBlank()) {
                            Text("Notes", style = MaterialTheme.typography.titleMedium)
                            Text(mem.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (mem.userProfile.isBlank() && mem.notes.isBlank()) {
                            Text("No memory data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                else -> Text("No memory data", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}