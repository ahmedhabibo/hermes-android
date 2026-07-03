package com.hermes.android.ui.chat

import android.content.Context
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showModelPicker by remember { mutableStateOf(false) }

    val attachmentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addAttachment(it) }
    }

    // Auto-scroll to bottom when messages change
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    // Show error snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Dismiss",
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.sessionTitle.ifBlank { "Chat" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Model picker
                    if (state.availableModels.isNotEmpty()) {
                        TextButton(onClick = { showModelPicker = true }) {
                            Text(
                                text = state.selectedModel.take(12),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = showModelPicker,
                            onDismissRequest = { showModelPicker = false }
                        ) {
                            state.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        viewModel.updateModel(model)
                                        showModelPicker = false
                                    }
                                )
                            }
                        }
                    }
                    // Attach button
                    IconButton(
                        onClick = { attachmentLauncher.launch("*/*") },
                        enabled = !state.isSending
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Attach file")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages list
            if (state.messages.isEmpty() && !state.isSending) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Start a conversation",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = state.messages,
                        key = { it.id }
                    ) { message ->
                        when (message.role) {
                            "user" -> UserMessage(message = message, context = context)
                            "assistant" -> AssistantMessage(message = message, context = context)
                            else -> ToolMessage(message = message)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // Pending attachments preview
            if (state.pendingAttachments.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.pendingAttachments.forEach { uri ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Pending attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.clearAttachments() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear attachments")
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { viewModel.updateInput(it) },
                    placeholder = { Text("Type a message...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 5,
                    shape = RoundedCornerShape(24.dp),
                    enabled = !state.isSending,
                    trailingIcon = {
                        if (state.inputText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateInput("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear text",
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        viewModel.sendMessage(sessionId)
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(
                            (context as? android.app.Activity)?.currentFocus?.windowToken,
                            0
                        )
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (state.isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserMessage(message: ChatMessage, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Render image attachments
        message.attachments.forEach { att ->
            if (att.startsWith("content://") || att.startsWith("file://") || att.startsWith("http")) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(att)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 4.dp)
                )
            }
        }
        // Text bubble
        if (message.content.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.content,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun AssistantMessage(message: ChatMessage, context: android.content.Context) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Render image attachments
        message.attachments.forEach { att ->
            if (att.startsWith("content://") || att.startsWith("file://") || att.startsWith("http")) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(att)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Attachment",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .sizeIn(maxWidth = 200.dp, maxHeight = 200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 4.dp)
                )
            }
        }
        // Text bubble with markdown
        if (message.content.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                MarkdownText(
                    text = message.content,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        if (message.isStreaming && message.content.isBlank()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(16.dp)
                    .padding(8.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
fun ToolMessage(message: ChatMessage) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = message.content,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
