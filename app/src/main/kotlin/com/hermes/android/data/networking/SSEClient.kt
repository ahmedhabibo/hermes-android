package com.hermes.android.data.networking

import com.hermes.android.HermesApp
import com.hermes.android.data.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

// SSE client using OkHttp's EventSource — port of Hermex SSEClient.swift
// Handles all hermes-webui SSE event types: token, reasoning, tool, tool_complete,
// title, done, interim_assistant, error, cancel, stream_end, and heartbeats

class SSEClient(
    private val client: OkHttpClient = HermesApp.instance.httpClient
) {
    private var eventSource: EventSource? = null

    // Returns a Flow of SSEEvent that stays active until stream_end, cancel, or error
    fun connect(url: String, headers: Map<String, String> = emptyMap()): Flow<SSEEvent> = callbackFlow {
        val factory = EventSources.createFactory(client)

        val reqBuilder = Request.Builder().url(url)
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache, no-transform")
            .addHeader("Accept-Encoding", "identity")

        headers.forEach { (k, v) -> reqBuilder.addHeader(k, v) }

        val request = reqBuilder.build()

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type == null) {
                    // Heartbeat or comment — ignore
                    return
                }
                val event = decodeSSEEvent(type, data)
                trySend(event)

                // Terminal events — close the flow
                when (event) {
                    is SSEEvent.StreamEnd, is SSEEvent.Cancelled, is SSEEvent.Error -> {
                        eventSource.cancel()
                        close()
                    }
                    else -> {}
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                trySend(SSEEvent.TransportError(t?.message ?: "SSE connection failed"))
                close()
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection established
            }
        }

        eventSource = factory.newEventSource(request, listener)

        awaitClose {
            eventSource?.cancel()
        }
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }

    companion object {
        // Reuse the shared JSON decoder for consistency
        private val json = hermesJson

        // Decode SSE event type + data string into typed SSEEvent
        // Port of Swift SSEEventDecoder.decode()
        fun decodeSSEEvent(eventType: String, data: String): SSEEvent {
            return try {
                when (eventType) {
                    "token" -> {
                        val payload = json.decodeFromString<TokenPayload>(data.ifEmpty { "{}" })
                        SSEEvent.Token(payload.text ?: "")
                    }
                    "reasoning" -> {
                        val payload = json.decodeFromString<ReasoningPayload>(data.ifEmpty { "{}" })
                        SSEEvent.Reasoning(payload.text ?: "")
                    }
                    "tool" -> {
                        val payload = json.decodeFromString<ToolPayload>(data.ifEmpty { "{}" })
                        SSEEvent.ToolStarted(
                            eventType = payload.eventType,
                            name = payload.name,
                            preview = payload.preview,
                            args = payload.args,
                            duration = payload.duration,
                            isError = payload.isError,
                            stableId = payload.stableId
                        )
                    }
                    "tool_complete" -> {
                        val payload = json.decodeFromString<ToolPayload>(data.ifEmpty { "{}" })
                        SSEEvent.ToolCompleted(
                            eventType = payload.eventType,
                            name = payload.name,
                            preview = payload.preview,
                            args = payload.args,
                            duration = payload.duration,
                            isError = payload.isError,
                            stableId = payload.stableId
                        )
                    }
                    "title" -> {
                        val payload = json.decodeFromString<TitlePayload>(data.ifEmpty { "{}" })
                        SSEEvent.Title(payload.sessionId, payload.title)
                    }
                    "done" -> {
                        val payload = json.decodeFromString<DonePayload>(data.ifEmpty { "{}" })
                        SSEEvent.Done(
                            usage = ContextWindowSnapshot(
                                contextLength = payload.contextLength ?: 0,
                                thresholdTokens = payload.thresholdTokens ?: 0,
                                lastPromptTokens = payload.lastPromptTokens ?: 0,
                                inputTokens = payload.inputTokens ?: 0,
                                outputTokens = payload.outputTokens ?: 0,
                                estimatedCost = payload.estimatedCost ?: 0.0
                            )
                        )
                    }
                    "interim_assistant" -> {
                        val payload = json.decodeFromString<InterimAssistantPayload>(data.ifEmpty { "{}" })
                        SSEEvent.InterimAssistant(payload.text, payload.alreadyStreamed)
                    }
                    "stream_end" -> SSEEvent.StreamEnd
                    "cancel" -> SSEEvent.Cancelled
                    "error" -> {
                        val payload = json.decodeFromString<ErrorPayload>(data.ifEmpty { "{}" })
                        SSEEvent.Error(payload.error ?: payload.message ?: "The stream returned an error.")
                    }
                    else -> SSEEvent.Ignored
                }
            } catch (e: Exception) {
                SSEEvent.Error("Failed to decode SSE event '$eventType': ${e.message}")
            }
        }
    }
}
