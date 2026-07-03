package com.hermes.android.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hermes.android.data.models.SessionSummary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionListScreen(
    onSessionClick: (String) -> Unit,
    onNewSession: (String) -> Unit,
    onDisconnect: () -> Unit,
    onNavigateToMemory: () -> Unit
) {
    val viewModel: SessionListViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<SessionSummary?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Error snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
            viewModel.dismissError()
        }
    }

    // Filter + sort: pinned first, then by updatedAt desc, filter by query
    val visibleSessions = remember(state.sessions, searchQuery) {
        state.sessions
            .filter { searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) }
            .sortedWith(compareByDescending<SessionSummary> { it.pinned }.thenByDescending { it.updatedAt })
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search sessions...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { isSearching = false; searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Close search")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Sessions") },
                    actions = {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Memory") },
                                onClick = { showMenu = false; onNavigateToMemory() }
                            )
                            DropdownMenuItem(
                                text = { Text("Disconnect") },
                                onClick = { showMenu = false; onDisconnect() }
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSearching) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.newSession(onNewSession) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "New session") },
                    text = { Text("New") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.sessions.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null && state.sessions.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
                visibleSessions.isEmpty() -> {
                    Text(
                        text = if (searchQuery.isNotEmpty())
                            "No sessions match \"$searchQuery\""
                        else
                            "No sessions yet.\nTap + to start a new conversation.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = visibleSessions,
                            key = { it.id }
                        ) { session ->
                            SessionRow(
                                session = session,
                                onClick = { onSessionClick(session.id) },
                                onRename = { showRenameDialog = session },
                                onDelete = { viewModel.deleteSession(session.id) },
                                onPin = { viewModel.pinSession(session.id, !session.pinned) },
                                onArchive = { viewModel.archiveSession(session.id, !session.archived) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    showRenameDialog?.let { session ->
        var newTitle by remember { mutableStateOf(session.title) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text("Rename session") },
            text = {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameSession(session.id, newTitle)
                    showRenameDialog = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionRow(
    session: SessionSummary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPin: () -> Unit,
    onArchive: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val timeStr = if (session.updatedAt > 0) dateFormat.format(Date(session.updatedAt.toLong() * 1000)) else ""
    var showRowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (session.pinned) Text("\uD83D\uDCCC", modifier = Modifier.padding(end = 8.dp))
        if (session.archived) Text("\uD83D\uDCE6", modifier = Modifier.padding(end = 4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                if (session.model.isNotEmpty()) Text(
                    session.model,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.messageCount > 0) Text(
                    "${session.messageCount} msgs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (timeStr.isNotEmpty()) Text(
                    timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (session.isStreaming) CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp
        )
        IconButton(onClick = { showRowMenu = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }
        DropdownMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
            DropdownMenuItem(text = { Text("Rename") }, onClick = { showRowMenu = false; onRename() })
            DropdownMenuItem(text = { Text(if (session.pinned) "Unpin" else "Pin") }, onClick = { showRowMenu = false; onPin() })
            DropdownMenuItem(text = { Text(if (session.archived) "Unarchive" else "Archive") }, onClick = { showRowMenu = false; onArchive() })
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = { showRowMenu = false; onDelete() }
            )
        }
    }
}
