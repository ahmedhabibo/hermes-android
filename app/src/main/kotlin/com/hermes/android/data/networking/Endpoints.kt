package com.hermes.android.data.networking

import java.io.UnsupportedEncodingException
import java.net.URLEncoder

// Base endpoint class
open class Endpoint(open val path: String) {
    fun buildUrl(baseUrl: String, queryItems: List<Pair<String, String>> = emptyList()): String {
        val url = "${baseUrl.trimEnd('/')}$path"
        if (queryItems.isEmpty()) return url
        try {
            val query = queryItems.joinToString("&") { (k, v) ->
                "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
            }
            return "$url?$query"
        } catch (e: UnsupportedEncodingException) {
            return url
        }
    }

    open fun queryItems(): List<Pair<String, String>> = emptyList()

    fun fullUrl(baseUrl: String): String = buildUrl(baseUrl, queryItems())
}

// ── Auth & health ──
object Health : Endpoint("/health")
object AuthStatus : Endpoint("/api/auth/status")
object Login : Endpoint("/api/auth/login")
object Logout : Endpoint("/api/auth/logout")

// ── Sessions ──
object Sessions : Endpoint("/api/sessions")
data class SessionsSearch(
    val query: String,
    val content: Boolean,
    val depth: Int
) : Endpoint("/api/sessions/search") {
    override fun queryItems(): List<Pair<String, String>> = listOf(
        "q" to query,
        "content" to if (content) "1" else "0",
        "depth" to depth.toString()
    )
}
data class SessionDetailEndpoint(
    val id: String,
    val includeMessages: Boolean = true,
    val messageLimit: Int? = null,
    val messageBefore: Int? = null,
    val expandRenderable: Boolean = false
) : Endpoint("/api/session") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("session_id" to id)
        add("messages" to if (includeMessages) "1" else "0")
        messageLimit?.let { add("msg_limit" to it.toString()) }
        messageBefore?.let { add("msg_before" to it.toString()) }
        if (expandRenderable) add("expand_renderable" to "1")
    }
}
data class SessionStatus(val id: String) : Endpoint("/api/session/status") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to id)
}
object NewSession : Endpoint("/api/session/new")
data class RenameSession(val sessionId: String, val title: String) : Endpoint("/api/session/rename") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId, "title" to title)
}
data class DeleteSession(val sessionId: String) : Endpoint("/api/session/delete") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId)
}
data class PinSession(val sessionId: String, val pinned: Boolean) : Endpoint("/api/session/pin") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId, "pinned" to pinned.toString())
}
data class ArchiveSession(val sessionId: String, val archived: Boolean) : Endpoint("/api/session/archive") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId, "archived" to archived.toString())
}
data class BranchSession(val sessionId: String, val keepCount: Int? = null, val title: String? = null) : Endpoint("/api/session/branch") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("session_id" to sessionId)
        keepCount?.let { add("keep_count" to it.toString()) }
        title?.let { add("title" to it) }
    }
}
object CompressSession : Endpoint("/api/session/compress")
object UndoSession : Endpoint("/api/session/undo")
object RetrySession : Endpoint("/api/session/retry")
object TruncateSession : Endpoint("/api/session/truncate")
object UpdateSession : Endpoint("/api/session/update")
object MoveSession : Endpoint("/api/session/move")

// ── Projects ──
object Projects : Endpoint("/api/projects")
object CreateProject : Endpoint("/api/projects/create")

// ── Chat ──
object ChatStart : Endpoint("/api/chat/start")
data class ChatStream(val streamId: String) : Endpoint("/api/chat/stream") {
    override fun queryItems(): List<Pair<String, String>> = listOf("stream_id" to streamId)
}
data class ChatCancel(val streamId: String) : Endpoint("/api/chat/cancel") {
    override fun queryItems(): List<Pair<String, String>> = listOf("stream_id" to streamId)
}
data class ChatStreamStatus(val streamId: String) : Endpoint("/api/chat/stream/status") {
    override fun queryItems(): List<Pair<String, String>> = listOf("stream_id" to streamId)
}
object ChatSteer : Endpoint("/api/chat/steer")

// ── Workspace / files ──
object Workspaces : Endpoint("/api/workspaces")
data class WorkspaceSuggestions(val prefix: String) : Endpoint("/api/workspaces/suggest") {
    override fun queryItems(): List<Pair<String, String>> = listOf("prefix" to prefix)
}
data class DirectoryList(val sessionId: String, val dirPath: String?) : Endpoint("/api/list") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("session_id" to sessionId)
        dirPath?.let { add("path" to it) }
    }
}
data class File(val sessionId: String, val filePath: String) : Endpoint("/api/file") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId, "path" to filePath)
}
data class RawFile(val sessionId: String, val filePath: String) : Endpoint("/api/file/raw") {
    override fun queryItems(): List<Pair<String, String>> = listOf("session_id" to sessionId, "path" to filePath)
}

// ── Models / providers / profiles / reasoning ──
object Models : Endpoint("/api/models")
object Commands : Endpoint("/api/commands")
object DefaultModel : Endpoint("/api/default-model")
object Reasoning : Endpoint("/api/reasoning")
object Personalities : Endpoint("/api/personalities")
object SetPersonality : Endpoint("/api/personality/set")
object Profiles : Endpoint("/api/profiles")
object SwitchProfile : Endpoint("/api/profile/switch")
object Providers : Endpoint("/api/providers")
object Settings : Endpoint("/api/settings")

// ── Read-only server panels ──
object Crons : Endpoint("/api/crons")
data class CronStatus(val jobId: String?) : Endpoint("/api/crons/status") {
    override fun queryItems(): List<Pair<String, String>> = jobId?.let { listOf("job_id" to it) } ?: emptyList()
}
data class CronOutput(val jobId: String, val limit: Int?) : Endpoint("/api/crons/output") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("job_id" to jobId)
        limit?.let { add("limit" to it.toString()) }
    }
}
object Memory : Endpoint("/api/memory")
object Skills : Endpoint("/api/skills")
data class SkillContent(val name: String, val file: String?) : Endpoint("/api/skills/content") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("name" to name)
        file?.let { add("file" to it) }
    }
}

// ── Upload ──
object Upload : Endpoint("/api/upload")