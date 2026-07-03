package com.hermes.android.ui.chat

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hermes.android.data.models.ChatMessage
import com.hermes.android.data.models.MessageAttachment
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onSessionClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var pendingUris by remember { mutableStateOf<List<String>>(emptyList()) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            pendingUris = uris.map { it.toString() }
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    // Load session messages + models on init
    LaunchedEffect(Unit) {
        viewModel.loadMessages()
        viewModel.loadModels()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(
                            text = state.title.ifBlank { "Chat" },
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.modelName.isNotBlank()) {
                            Text(
                                text = state.modelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                actions = {
                    // Model picker dropdown
                    var modelMenuExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { modelMenuExpanded = true }) {
                            Text(
                                text = state.selectedModel ?: "Model",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            state.models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text("${model.name} (${model.id})") },
                                    onClick = {
                                        viewModel.selectModel(model.id)
                                        modelMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onSessionClick(state.sessionId) }) {
                        Icon(Icons.Default.Info, contentDescription = "Session info")
                    }
                }
            )
        },
        bottomBar = {
            ChatInputBar(
                text = messageText,
                onTextChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank() || pendingUris.isNotEmpty()) {
                        viewModel.sendMessage(messageText, pendingUris)
                        messageText = ""
                        pendingUris = emptyList()
                    }
                },
                onAttach = { imagePicker.launch("image/*") },
                pendingCount = pendingUris.size
            )
        }
    ) { padding ->
        if (state.isLoading && state.messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
                if (state.isStreaming) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Text content
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Reasoning
                if (message.reasoning.isNotBlank()) {
                    var expanded by remember { mutableStateOf(false) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Reasoning",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                    if (expanded) {
                        Text(
                            text = message.reasoning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // Image attachments
                message.attachments.filter { it.isImage }.forEach { attachment ->
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = attachment.path,
                        contentDescription = attachment.name,
                        modifier = Modifier
                            .sizeIn(maxHeight = 200.dp, maxWidth = 280.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Non-image attachments
                message.attachments.filterNot { it.isImage }.forEach { attachment ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "📎 ${attachment.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    pendingCount: Int
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pendingCount > 0) {
                Text(
                    text = "📎 $pendingCount",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            IconButton(onClick = onAttach) {
                Icon(Icons.Default.Add, contentDescription = "Attach image")
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Send a message…") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(onClick = onSend, enabled = text.isNotBlank() || pendingCount > 0) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank() || pendingCount > 0)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
