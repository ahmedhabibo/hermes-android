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

sealed class ApiError(message: String) : Exception(message) {
    class Network(val underlying: Throwable) : ApiError(underlying.message ?: "Network error")
    class Http(val statusCode: Int, val body: String?) : ApiError("HTTP $statusCode: ${body?.take(200)}")
    data object Unauthorized : ApiError("Unauthorized — please reconnect")
}

class ApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient = HermesApp.instance.httpClient
) {
    private val json = hermesJson
    private val jsonMediaType = "application/json".toMediaType()

    private suspend fun request(
        endpoint: Endpoint,
        method: String,
        body: String? = null
    ): String = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            val url = endpoint.fullUrl(baseUrl)
            val reqBuilder = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")

            val requestBody = when {
                method == "GET" || method == "DELETE" -> null
                body != null -> RequestBody.create(jsonMediaType, body)
                else -> RequestBody.create(jsonMediaType, "")
            }
            reqBuilder.method(method, requestBody)

            client.newCall(reqBuilder.build()).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val responseBody = response.body?.string() ?: ""
                        if (response.code == 401) {
                            if (cont.isActive) cont.resumeWithException(ApiError.Unauthorized)
                            return
                        }
                        if (response.code !in 200..299) {
                            if (cont.isActive) cont.resumeWithException(ApiError.Http(response.code, responseBody))
                            return
                        }
                        if (cont.isActive) cont.resume(responseBody)
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                    }
                }
            })
        }
    }

    // ── Auth ──
    suspend fun passwordLogin(username: String, password: String): String =
        request(PasswordLogin, "POST", json.encodeToString(PasswordLoginRequest(username, password)))

    suspend fun authMe(): AuthMeResponse =
        json.decodeFromString(request(AuthMe, "GET"))

    suspend fun authLogout(): AuthLogoutResponse =
        json.decodeFromString(request(AuthLogout, "POST"))

    // ── Sessions ──
    suspend fun getSessions(): SessionsResponse =
        json.decodeFromString(request(Sessions, "GET"))

    suspend fun getSessionMessages(sessionId: String): SessionMessagesResponse =
        json.decodeFromString(request(SessionMessagesEndpoint(sessionId), "GET"))

    suspend fun newSession(workspace: String? = null, model: String? = null): NewSessionResponse =
        json.decodeFromString(
            request(Sessions, "POST", json.encodeToString(NewSessionRequest(workspace, model)))
        )

    suspend fun renameSession(sessionId: String, title: String): SessionMutationResponse =
        json.decodeFromString(
            request(
                SessionDetailEndpoint(sessionId, includeMessages = false),
                "PATCH",
                json.encodeToString(RenameSessionRequest(title = title))
            )
        )

    suspend fun deleteSession(sessionId: String): SessionMutationResponse =
        json.decodeFromString(request(SessionDetailEndpoint(sessionId, false), "DELETE"))

    suspend fun archiveSession(sessionId: String, archive: Boolean): SessionMutationResponse =
        json.decodeFromString(
            request(
                SessionDetailEndpoint(sessionId, false),
                "PATCH",
                json.encodeToString(ArchiveSessionRequest(archived = archive))
            )
        )

    // ── Models ──
    suspend fun getModelOptions(): ModelOptionsResponse =
        json.decodeFromString(request(ModelOptions, "GET"))

    suspend fun getDefaultModel(): DefaultModelResponse =
        json.decodeFromString(request(DefaultModel, "GET"))

    // ── Memory ──
    suspend fun getMemory(): MemoryResponse =
        json.decodeFromString(request(MemoryStatus, "GET"))

    // ── Skills ──
    suspend fun getSkills(): SkillsResponse =
        json.decodeFromString(request(SkillsList, "GET"))

    // ── Crons ──
    suspend fun getCronJobs(): CronsResponse =
        json.decodeFromString(request(CronJobs, "GET"))

    // ── Upload ──
    suspend fun uploadFile(sessionId: String, fileName: String, file: java.io.File): UploadResponse =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { cont ->
                val url = FileUpload.fullUrl(baseUrl)
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("session_id", sessionId)
                    .addFormDataPart("file", fileName,
                        RequestBody.create("application/octet-stream".toMediaType(), file))
                    .build()

                client.newCall(
                    Request.Builder().url(url).post(body).build()
                ).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                    }
                    override fun onResponse(call: Call, response: Response) {
                        try {
                            val resp = response.body?.string() ?: ""
                            if (response.code !in 200..299) {
                                if (cont.isActive) cont.resumeWithException(
                                    ApiError.Http(response.code, resp))
                                return
                            }
                            if (cont.isActive) cont.resume(json.decodeFromString(resp))
                        } catch (e: Exception) {
                            if (cont.isActive) cont.resumeWithException(ApiError.Network(e))
                        }
                    }
                })
            }
        }
}

// ── Skill content endpoint ──
data class SkillContentEndpoint(
    val name: String,
    val file: String? = null
) : Endpoint("/api/skills/content") {
    override fun queryItems(): List<Pair<String, String>> = buildList {
        add("name" to name)
        file?.let { add("file" to it) }
    }
}
