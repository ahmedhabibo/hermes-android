package com.hermes.android.data.networking

import java.net.URLEncoder

/**
 * Endpoint definitions matching the REAL Hermes WebUI server API
 * (hermes_cli/web_server.py + dashboard_auth/routes.py).
 *
 * Key changes from the fabricated version:
 *  - Sessions: RESTful /api/sessions/{id} with GET, PATCH (rename/archive), DELETE
 *  - Auth: POST /auth/password-login (basic auth provider), GET /api/auth/me
 *  - Chat: WebSocket /api/pty (PTY-over-WebSocket), not REST + SSE
 *  - Models: GET /api/model/options (provider-grouped model lists)
 *  - Upload: POST /api/files/upload
 *  - Crons: GET /api/cron/jobs
 *  - Pin: REMOVED (no pin endpoint exists in the WebUI server)
 */

open class Endpoint(open val path: String) {
    fun buildUrl(baseUrl: String, queryItems: List<Pair<String, String>> = emptyList()): String {
        val url = "${baseUrl.trimEnd('/')}$path"
        if (queryItems.isEmpty()) return url
        val query = queryItems.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }
        return "$url?$query"
    }

    open fun queryItems(): List<Pair<String, String>> = emptyList()
    fun fullUrl(baseUrl: String): String = buildUrl(baseUrl, queryItems())
}

// ── Auth (dashboard_auth/routes.py) ──
object PasswordLogin : Endpoint("/auth/password-login")
object AuthMe : Endpoint("/api/auth/me")
object AuthLogout : Endpoint("/auth/logout")
object AuthProviders : Endpoint("/api/auth/providers")

// ── Sessions ──
object Sessions : Endpoint("/api/sessions")
data class SessionDetailEndpoint(
    val id: String,
    val includeMessages: Boolean = true,
    val messageLimit: Int? = 50
) : Endpoint("/api/sessions/{id}".replace("{id}", id)) {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("include_messages" to if (includeMessages) "1" else "0")
        messageLimit?.let { add("message_limit" to it.toString()) }
    }
}
data class SessionMessagesEndpoint(
    val sessionId: String
) : Endpoint("/api/sessions/{sessionId}/messages".replace("{sessionId}", sessionId))
data class NewSessionEndpoint(
    val workspace: String? = null,
    val model: String? = null
) : Endpoint("/api/sessions/new") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        workspace?.let { add("workspace" to it) }
        model?.let { add("model" to it) }
    }
}
data class RenameSessionEndpoint(
    val sessionId: String,
    val title: String,
    val profile: String? = null
) : Endpoint("/api/sessions/{sessionId}".replace("{sessionId}", sessionId)) {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("title" to title)
        profile?.let { add("profile" to it) }
    }
}
data class DeleteSessionEndpoint(
    val sessionId: String
) : Endpoint("/api/sessions/{sessionId}".replace("{sessionId}", sessionId))
data class ArchiveSessionEndpoint(
    val sessionId: String,
    val archive: Boolean,
    val profile: String? = null
) : Endpoint("/api/sessions/{sessionId}".replace("{sessionId}", sessionId)) {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("archived" to if (archive) "1" else "0")
        profile?.let { add("profile" to it) }
    }
}

// Note: No PIN endpoint exists in the WebUI API

// ── Chat (WebSocket endpoint) ──
object ChatWebSocket : Endpoint("/api/pty")

// ── Models ──
object ModelOptions : Endpoint("/api/model/options")
object DefaultModel : Endpoint("/api/model/recommended-default")
object ModelSet : Endpoint("/api/model/set")
data class ModelSetEndpoint(
    val provider: String,
    val model: String
) : Endpoint("/api/model/set") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("provider" to provider)
        add("model" to model)
    }
}

// ── Files ──
object FileUpload : Endpoint("/api/files/upload")
data class FileUploadEndpoint(
    val sessionId: String,
    val fileName: String
) : Endpoint("/api/files/upload") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("session_id" to sessionId)
        add("filename" to fileName)
    }
}

// ── Memory ──
object MemoryStatus : Endpoint("/api/memory")

// ── Skills ──
object SkillsList : Endpoint("/api/skills")
object SkillContent : Endpoint("/api/skills/content")

// ── Crons ──
object CronJobs : Endpoint("/api/cron/jobs")
data class CronJobEndpoint(
    val jobId: String
) : Endpoint("/api/cron/jobs/{jobId}".replace("{jobId}", jobId))
data class CronJobRunEndpoint(
    val jobId: String
) : Endpoint("/api/cron/jobs/{jobId}/run".replace("{jobId}", jobId))
data class CronJobPauseEndpoint(
    val jobId: String
) : Endpoint("/api/cron/jobs/{jobId}/pause".replace("{jobId}", jobId))
data class CronJobResumeEndpoint(
    val jobId: String
) : Endpoint("/api/cron/jobs/{jobId}/resume".replace("{jobId}", jobId))
data class CronJobDeleteEndpoint(
    val jobId: String
) : Endpoint("/api/cron/jobs/{jobId}".replace("{jobId}", jobId))

// ── Other (for completeness) ──
object Settings : Endpoint("/api/config")
object ProvidersList : Endpoint("/api/providers")
object ProfilesList : Endpoint("/api/profiles")
object WorkspacesList : Endpoint("/api/workspaces")