package com.hermes.android.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hermes.android.data.models.SessionSummary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    viewModel: SessionListViewModel,
    onSessionClick: (String) -> Unit,
    onNewSession: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error as snackbar
    LaunchedEffect(state.error) {
        if (state.error != null) {
            snackbarHostState.showSnackbar(
                message = state.error!!,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sessions") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNewSession) {
                        Icon(Icons.Default.Add, contentDescription = "New session")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when {
            state.isLoading && state.sessions.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    items(state.sessions, key = { it.sessionId }) { session ->
                        SessionRow(
                            session = session,
                            onClick = { onSessionClick(session.sessionId) },
                            onRename = { newTitle ->
                                viewModel.renameSession(session.sessionId, newTitle)
                            },
                            onDelete = { viewModel.deleteSession(session.sessionId) },
                            onArchive = { viewModel.archiveSession(session.sessionId, !session.archived) }
                        )
                    }
                    if (state.sessions.isEmpty() && !state.isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No sessions yet", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: SessionSummary,
    onClick: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    Column {
        ListItem(
            headlineContent = {
                Text(
                    text = if (session.title.isNotBlank()) session.title else "Untitled",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (session.archived) MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.outline
                    ) else MaterialTheme.typography.bodyMedium
                )
            },
            supportingContent = {
                Column {
                    Text(
                        "${session.messageCount} msgs · ${session.model.ifBlank { "default" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (session.updatedAt > 0) {
                        Text(
                            formatTime(session.updatedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = { showMenu = false; showRenameDialog = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (session.archived) "Unarchive" else "Archive") },
                            onClick = { showMenu = false; onArchive() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            },
            modifier = Modifier.clickable { onClick() }
        )
        HorizontalDivider()
    }

    if (showRenameDialog) {
        var newTitle by remember { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename session") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTitle.isNotBlank()) onRename(newTitle)
                    showRenameDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun formatTime(epochSeconds: Double): String {
    val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return sdf.format(Date((epochSeconds * 1000).toLong()))
}
