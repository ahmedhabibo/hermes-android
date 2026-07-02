package com.hermes.android.data.networking

import com.hermes.android.HermesApp
import com.hermes.android.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val hermesJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = false
}

class ApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient = HermesApp.instance.httpClient
) {
    private val json = hermesJson
    private val jsonMediaType = "application/json".toMediaType()

    private suspend fun request(endpoint: Endpoint, method: String, body: String? = null): String =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val url = endpoint.fullUrl(baseUrl)
                val reqBuilder = Request.Builder().url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("Cache-Control", "no-cache")

                val requestBody = when {
                    method == "GET" -> null
                    body != null -> RequestBody.create(jsonMediaType, body)
                    else -> RequestBody.create(jsonMediaType, "")
                }

                reqBuilder.method(method, requestBody)

                val req = reqBuilder.build()
                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                    }
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val responseBody = response.body?.string() ?: ""
                            if (response.code == 401) {
                                cont.resumeWithException(ApiError.Unauthorized)
                                return
                            }
                            if (response.code !in 200..299) {
                                cont.resumeWithException(ApiError.Http(response.code, responseBody))
                                return
                            }
                            cont.resume(responseBody)
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                        }
                    }
                })

                cont.invokeOnCancellation { client.dispatcher.cancelAll() }
            }
        }

    private suspend fun get(endpoint: Endpoint): String = request(endpoint, "GET")
    private suspend fun post(endpoint: Endpoint, body: String? = null): String = request(endpoint, "POST", body)

    // ── Auth & health ──
    suspend fun health(): HealthResponse =
        json.decodeFromString(get(Health))

    suspend fun authStatus(): AuthStatusResponse =
        json.decodeFromString(get(AuthStatus))

    suspend fun login(password: String): LoginResponse =
        json.decodeFromString(post(Login, json.encodeToString(LoginRequest(password))))

    suspend fun logout(): LoginResponse =
        json.decodeFromString(post(Logout))

    // ── Sessions ──
    suspend fun getSessions(): SessionsResponse =
        json.decodeFromString(get(Sessions))

    suspend fun getSession(id: String, includeMessages: Boolean = true, messageLimit: Int? = 50): SessionDetail =
        json.decodeFromString(get(SessionDetailEndpoint(id, includeMessages, messageLimit)))

    suspend fun getSessionStatus(id: String): SessionStatusResponse =
        json.decodeFromString(get(SessionStatus(id)))

    suspend fun newSession(workspace: String? = null, model: String? = null): SessionMutationResponse =
        json.decodeFromString(post(NewSession, json.encodeToString(NewSessionRequest(workspace, model))))

    suspend fun renameSession(sessionId: String, title: String): SessionMutationResponse =
        json.decodeFromString(post(RenameSession(sessionId, title), json.encodeToString(RenameSessionRequest(sessionId, title))))

    suspend fun deleteSession(sessionId: String): SessionMutationResponse =
        json.decodeFromString(post(DeleteSession(sessionId), json.encodeToString(DeleteSessionRequest(sessionId))))

    suspend fun pinSession(sessionId: String, pinned: Boolean): SessionMutationResponse =
        json.decodeFromString(post(PinSession(sessionId, pinned), json.encodeToString(PinSessionRequest(sessionId, pinned))))

    suspend fun archiveSession(sessionId: String, archived: Boolean): SessionMutationResponse =
        json.decodeFromString(post(ArchiveSession(sessionId, archived), json.encodeToString(ArchiveSessionRequest(sessionId, archived))))

    suspend fun branchSession(sessionId: String, keepCount: Int? = null, title: String? = null): SessionBranchResponse =
        json.decodeFromString(post(BranchSession(sessionId, keepCount, title), json.encodeToString(BranchSessionRequest(sessionId, keepCount, title))))

    // ── Chat ──
    suspend fun chatStart(sessionId: String, message: String, workspace: String? = null, model: String? = null, attachments: List<String>? = null): ChatStartResponse =
        json.decodeFromString(post(ChatStart, json.encodeToString(ChatStartRequest(sessionId, message, workspace ?: "", model ?: "", attachments ?: emptyList()))))

    suspend fun chatCancel(streamId: String): String = get(ChatCancel(streamId))

    suspend fun chatStreamStatus(streamId: String): String = get(ChatStreamStatus(streamId))

    suspend fun chatSteer(sessionId: String, text: String): String =
        json.decodeFromString(post(ChatSteer, json.encodeToString(ChatSteerRequest(sessionId, text))))

    // ── Models / providers / profiles ──
    suspend fun getModels(): ModelsResponse =
        json.decodeFromString(get(Models))

    suspend fun getProviders(): ProvidersResponse =
        json.decodeFromString(get(Providers))

    suspend fun getProfiles(): ProfilesResponse =
        json.decodeFromString(get(Profiles))

    suspend fun getSettings(): ServerSettings =
        json.decodeFromString(get(Settings))

    suspend fun getReasoning(): ReasoningResponse =
        json.decodeFromString(get(Reasoning))

    // ── Workspace ──
    suspend fun getWorkspaces(): WorkspacesResponse =
        json.decodeFromString(get(Workspaces))

    suspend fun listDirectory(sessionId: String, path: String?): DirectoryListResponse =
        json.decodeFromString(get(DirectoryList(sessionId, path)))

    suspend fun getFile(sessionId: String, path: String): FileResponse =
        json.decodeFromString(get(File(sessionId, path)))

    // ── Read-only panels ──
    suspend fun getCrons(): CronsResponse =
        json.decodeFromString(get(Crons))

    suspend fun getSkills(): SkillsResponse =
        json.decodeFromString(get(Skills))

    suspend fun getMemory(): MemoryResponse =
        json.decodeFromString(get(Memory))

    // ── Upload ──
    suspend fun uploadFile(sessionId: String, file: java.io.File, mimeType: String): UploadResponse =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val mediaType = mimeType.toMediaType()
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("session_id", sessionId)
                    .addFormDataPart("file", file.name, RequestBody.create(mediaType, file))
                    .build()

                val req = Request.Builder()
                    .url(Upload.fullUrl(baseUrl))
                    .post(body)
                    .addHeader("Accept", "application/json")
                    .build()

                client.newCall(req).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                    }
                    override fun onResponse(call: Call, response: Response) {
                        val responseBody = response.body?.string() ?: ""
                        if (response.code !in 200..299) {
                            cont.resumeWithException(ApiError.Http(response.code, responseBody))
                            return
                        }
                        cont.resume(json.decodeFromString(responseBody))
                    }
                })
            }
        }

    // SSE stream URL
    fun streamUrl(streamId: String): String = ChatStream(streamId).fullUrl(baseUrl)
}

// API errors
sealed class ApiError(message: String) : Exception(message) {
    class Network(val underlying: Throwable) : ApiError(underlying.message ?: "Network error")
    class Http(val statusCode: Int, val body: String?) : ApiError("HTTP $statusCode: ${body?.take(200)}")
    data object Unauthorized : ApiError("Unauthorized — please reconnect")
    class Decoding(val underlying: Throwable) : ApiError("Decoding error: ${underlying.message}")
}