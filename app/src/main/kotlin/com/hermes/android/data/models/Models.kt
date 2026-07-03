package com.hermes.android.data.models

import kotlinx.serialization.Serializable

// ── Sessions ──

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
    val matchType: String = "",
    val isArchived: Boolean = false
) {
    val id: String
        get() = if (sessionId.isNotEmpty()) sessionId else "session-${createdAt.toLong()}"

    val isCronSession: Boolean
        get() = sessionSource.equals("cron", ignoreCase = true) ||
                sourceTag.equals("cron", ignoreCase = true)
}

@Serializable
data class SessionsResponse(
    val sessions: List<SessionSummary> = emptyList(),
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)

@Serializable
data class SessionMessagesResponse(
    val sessionId: String = "",
    val messages: List<ChatMessage> = emptyList()
)

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
    val attachments: List<MessageAttachment> = emptyList()
) {
    val id: String
        get() = if (messageId.isNotEmpty()) messageId else "${role}-${timestamp}"
}

@Serializable
data class MessageAttachment(
    val name: String = "",
    val path: String = "",
    val mime: String = "",
    val size: Long = 0,
    val isImage: Boolean = false
)

// ── Auth ──

@Serializable
data class PasswordLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthMeResponse(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val provider: String = "",
    val expiresAt: Long = 0
)

@Serializable
data class AuthLogoutResponse(
    val status: String = "ok"
)

// ── Session mutations ──

@Serializable
data class NewSessionRequest(
    val workspace: String? = null,
    val model: String? = null
)

@Serializable
data class NewSessionResponse(
    val sessionId: String = "",
    val ok: Boolean = false
)

@Serializable
data class RenameSessionRequest(
    val title: String? = null,
    val archived: Boolean? = null,
    val profile: String? = null
)

@Serializable
data class ArchiveSessionRequest(
    val title: String? = null,
    val archived: Boolean = false,
    val profile: String? = null
)

@Serializable
data class SessionMutationResponse(
    val ok: Boolean = false,
    val title: String = "",
    val archived: Boolean = false
)

// ── Models ──

@Serializable
data class ModelOptionsResponse(
    val providers: List<ProviderOption> = emptyList()
)

@Serializable
data class ProviderOption(
    val id: String = "",
    val label: String = "",
    val authenticated: Boolean = false,
    val models: List<ModelInfo> = emptyList()
)

@Serializable
data class ModelInfo(
    val id: String = "",
    val name: String = "",
    val contextLength: Int = 0
)

@Serializable
data class DefaultModelResponse(
    val provider: String = "",
    val model: String = ""
)

// ── Memory ──

@Serializable
data class MemoryResponse(
    val provider: String = "",
    val providers: List<MemoryProviderInfo> = emptyList()
)

@Serializable
data class MemoryProviderInfo(
    val name: String = "",
    val description: String = "",
    val configured: Boolean = false
)

// ── Skills ──

@Serializable
data class SkillsResponse(
    val skills: List<SkillInfo> = emptyList()
)

@Serializable
data class SkillInfo(
    val name: String = "",
    val description: String = "",
    val enabled: Boolean = false,
    val category: String = ""
)

// ── Crons ──

@Serializable
data class CronsResponse(
    val jobs: List<CronJob> = emptyList()
)

@Serializable
data class CronJob(
    val id: String = "",
    val name: String = "",
    val schedule: String = "",
    val enabled: Boolean = true,
    val lastRun: Double = 0.0,
    val nextRun: Double = 0.0,
    val prompt: String = ""
)

// ── Upload ──

@Serializable
data class UploadResponse(
    val path: String = "",
    val filename: String = "",
    val size: Long = 0
)

// ── Context window (used by SSE Done event) ──

@Serializable
data class ContextWindowSnapshot(
    val contextLength: Int = 0,
    val thresholdTokens: Int = 0,
    val lastPromptTokens: Int = 0,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val estimatedCost: Double = 0.0
)

// ── Health ──

@Serializable
data class HealthResponse(
    val status: String = "ok"
)
