package com.hermes.android.data.models

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String = "ok",
    val version: String = "0.1.0"
)

@Serializable
data class SessionsResponse(
    val sessions: List<SessionSummary> = emptyList(),
    val cliCount: Int = 0,
    val serverTime: Double = 0.0,
    val serverTz: String = ""
)

@Serializable
data class SessionSummary(
    val sessionId: String = "",
    val title: String = "",
    val workspace: String = "",
    val model: String = "",
    val modelProvider: String = "",
    val messageCount: Int = 0,
    val createdAt: Double = 0.0,
    val updatedAt: Double = 0.0,
    val lastMessageAt: Double = 0.0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val projectId: String = "",
    val profile: String = "",
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val estimatedCost: Double = 0.0,
    val activeStreamId: String = "",
    val isStreaming: Boolean = false,
    val isCliSession: Boolean = false,
    val sourceTag: String = "",
    val sessionSource: String = "",
    val sourceLabel: String = "",
    val matchType: String = ""
) {
    val id: String
        get() = if (sessionId.isNotEmpty()) sessionId else "session-${if (title.isNotEmpty()) title else "untitled"}-${if (createdAt > 0) createdAt else if (updatedAt > 0) updatedAt else if (lastMessageAt > 0) lastMessageAt else 0}"

    val isCronSession: Boolean
        get() = sessionId.startsWith("cron_") ||
                listOf(sessionSource, sourceTag, sourceLabel)
                        .any { it.isNotEmpty() && it.equals("cron", ignoreCase = true) }
}

@Serializable
data class SessionDetail(
    val sessionId: String = "",
    val title: String = "",
    val workspace: String = "",
    val model: String = "",
    val modelProvider: String = "",
    val messageCount: Int = 0,
    val createdAt: Double = 0.0,
    val updatedAt: Double = 0.0,
    val lastMessageAt: Double = 0.0,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val projectId: String = "",
    val profile: String = "",
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val estimatedCost: Double = 0.0,
    val activeStreamId: String = "",
    val pendingUserMessage: String = "",
    val pendingAttachments: List<String> = emptyList(),
    val pendingStartedAt: Double = 0.0,
    val contextLength: Int = 0,
    val thresholdTokens: Int = 0,
    val lastPromptTokens: Int = 0,
    val isCliSession: Boolean = false,
    val messages: List<String> = emptyList(),
    val toolCalls: List<String> = emptyList(),
    val messagesTruncated: Boolean = false,
    val messagesOffset: Int = 0,
    val compressionAnchorVisibleIdx: Int = 0,
    val compressionAnchorSummary: String = ""
) {
    val id: String
        get() = if (sessionId.isNotEmpty()) sessionId else "session-${if (title.isNotEmpty()) title else "untitled"}-${if (createdAt > 0) createdAt else if (updatedAt > 0) updatedAt else if (lastMessageAt > 0) lastMessageAt else 0}"
}

@Serializable
data class ProjectSummary(
    val projectId: String = "",
    val name: String = "",
    val color: String = "",
    val createdAt: Double = 0.0
) {
    val id: String
        get() = if (projectId.isNotEmpty()) projectId else if (name.isNotEmpty()) name else java.util.UUID.randomUUID().toString()
}

@Serializable
data class ProjectsResponse(val projects: List<ProjectSummary> = emptyList())

@Serializable
data class ChatMessage(
    val role: String = "",
    val content: String = "",
    val timestamp: Double = 0.0,
    val messageId: String = "",
    val name: String = "",
    val toolCallId: String = "",
    val toolUseId: String = "",
    val toolCalls: List<String> = emptyList(),
    val contentParts: List<String> = emptyList(),
    val reasoning: String = "",
    val attachments: List<String> = emptyList()
) {
    val id: String
        get() = if (messageId.isNotEmpty()) messageId else "${if (role.isNotEmpty()) role else "unknown"}-${if (timestamp > 0) timestamp else 0}-${if (content.isNotEmpty()) content else ""}"
}

@Serializable
data class MessageAttachment(
    val name: String = "",
    val path: String = "",
    val mime: String = "",
    val size: Long = 0,
    val isImage: Boolean = false
)

@Serializable
data class LoginRequest(
    val password: String = ""
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val error: String = ""
)

@Serializable
data class ChatStartRequest(
    val sessionId: String = "",
    val message: String = "",
    val workspace: String = "",
    val model: String = "",
    val attachments: List<String> = emptyList()
)

@Serializable
data class ChatStartResponse(
    val streamId: String = "",
    val sessionId: String = ""
)

@Serializable
data class ChatSteerRequest(
    val sessionId: String = "",
    val text: String = ""
)

@Serializable
data class ServerSettings(
    val webuiVersion: String = "",
    val botName: String = "",
    val theme: String = ""
)

@Serializable
data class ModelsResponse(
    val models: List<ModelGroup> = emptyList()
)

@Serializable
data class ModelGroup(
    val provider: String = "",
    val models: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = "",
    val name: String = "",
    val provider: String = "",
    val contextLength: Int = 0
)

@Serializable
data class ProvidersResponse(
    val providers: List<String> = emptyList()
)

@Serializable
data class ProfilesResponse(
    val profiles: List<String> = emptyList()
)

@Serializable
data class ReasoningResponse(
    val effort: String = "",
    val display: String = ""
)

@Serializable
data class WorkspacesResponse(
    val workspaces: List<String> = emptyList()
)

@Serializable
data class WorkspaceEntry(
    val name: String = "",
    val path: String = "",
    val isDir: Boolean = false,
    val size: Long = 0
)

@Serializable
data class DirectoryListResponse(
    val entries: List<WorkspaceEntry> = emptyList()
)

@Serializable
data class FileResponse(
    val content: String = "",
    val path: String = "",
    val size: Long = 0,
    val mime: String = ""
)

@Serializable
data class CronsResponse(
    val jobs: List<CronJob> = emptyList()
)

@Serializable
data class CronJob(
    val jobId: String = "",
    val name: String = "",
    val schedule: String = "",
    val prompt: String = "",
    val enabled: Boolean = false,
    val lastRun: Double = 0.0,
    val nextRun: Double = 0.0,
    val lastOutput: String = "",
    val error: String = ""
)

@Serializable
data class SkillsResponse(
    val skills: List<SkillCategory> = emptyList()
)

@Serializable
data class SkillCategory(
    val name: String = "",
    val skills: List<SkillSummary> = emptyList()
)

@Serializable
data class SkillSummary(
    val name: String = "",
    val description: String = ""
)

@Serializable
data class MemoryResponse(
    val notes: String = "",
    val userProfile: String = "",
    val notesMtime: Double = 0.0,
    val profileMtime: Double = 0.0
)

@Serializable
data class UploadResponse(
    val filename: String = "",
    val path: String = "",
    val mime: String = "",
    val size: Long = 0,
    val isImage: Boolean = false
)

@Serializable
data class ContextWindowSnapshot(
    val contextLength: Int = 0,
    val thresholdTokens: Int = 0,
    val lastPromptTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val estimatedCost: Double = 0.0
)

// ── Request/response wrappers used by ApiClient ──

@Serializable
data class AuthStatusResponse(
    val authenticated: Boolean = false,
    val hasPassword: Boolean = false
)

@Serializable
data class SessionStatusResponse(
    val sessionId: String = "",
    val isStreaming: Boolean = false,
    val activeStreamId: String = ""
)

@Serializable
data class NewSessionRequest(
    val workspace: String? = null,
    val model: String? = null
)

@Serializable
data class SessionMutationResponse(
    val ok: Boolean = false,
    val sessionId: String = "",
    val error: String = ""
)

@Serializable
data class RenameSessionRequest(
    val sessionId: String = "",
    val title: String = ""
)

@Serializable
data class DeleteSessionRequest(
    val sessionId: String = ""
)

@Serializable
data class PinSessionRequest(
    val sessionId: String = "",
    val pinned: Boolean = false
)

@Serializable
data class ArchiveSessionRequest(
    val sessionId: String = "",
    val archived: Boolean = false
)

@Serializable
data class BranchSessionRequest(
    val sessionId: String = "",
    val keepCount: Int? = null,
    val title: String? = null
)

@Serializable
data class SessionBranchResponse(
    val ok: Boolean = false,
    val newSessionId: String = "",
    val error: String = ""
)