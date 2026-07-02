package com.hermes.android.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── SSE event types (port of Swift SSEEvent.swift) ──

sealed class SSEEvent {
    data class Token(val text: String) : SSEEvent()
    data class Reasoning(val text: String) : SSEEvent()
    data class ToolStarted(
        val eventType: String? = null,
        val name: String? = null,
        val preview: String? = null,
        val args: Map<String, String>? = null,
        val duration: Double? = null,
        val isError: Boolean? = null,
        val stableId: String? = null
    ) : SSEEvent()
    data class ToolCompleted(
        val eventType: String? = null,
        val name: String? = null,
        val preview: String? = null,
        val args: Map<String, String>? = null,
        val duration: Double? = null,
        val isError: Boolean? = null,
        val stableId: String? = null
    ) : SSEEvent()
    data class Title(val sessionId: String? = null, val title: String? = null) : SSEEvent()
    data class Done(val usage: ContextWindowSnapshot? = null) : SSEEvent()
    data class InterimAssistant(val text: String? = null, val alreadyStreamed: Boolean? = null) : SSEEvent()
    data class Error(val message: String) : SSEEvent()
    data class TransportError(val message: String) : SSEEvent()
    data object StreamEnd : SSEEvent()
    data object Cancelled : SSEEvent()
    data object Ignored : SSEEvent()
}

// SSE payload decoders — must be internal (not private) for kotlinx.serialization plugin access

@Serializable
internal data class TokenPayload(val text: String? = null)

@Serializable
internal data class ReasoningPayload(val text: String? = null)

@Serializable
internal data class ErrorPayload(val error: String? = null, val message: String? = null)

@Serializable
internal data class TitlePayload(
    @SerialName("session_id") val sessionId: String? = null,
    val title: String? = null
)

@Serializable
internal data class ToolPayload(
    @SerialName("event_type") val eventType: String? = null,
    val name: String? = null,
    val preview: String? = null,
    val args: Map<String, String>? = null,
    val duration: Double? = null,
    @SerialName("is_error") val isError: Boolean? = null,
    val tid: String? = null,
    val id: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
    @SerialName("tool_use_id") val toolUseId: String? = null,
    @SerialName("call_id") val callId: String? = null
) {
    val stableId: String?
        get() = listOf(tid, id, toolCallId, toolUseId, callId)
            .firstOrNull { !it.isNullOrBlank() }
}

@Serializable
internal data class InterimAssistantPayload(
    val text: String? = null,
    @SerialName("already_streamed") val alreadyStreamed: Boolean? = null
)

@Serializable
internal data class DonePayload(
    @SerialName("context_length") val contextLength: Int? = null,
    @SerialName("threshold_tokens") val thresholdTokens: Int? = null,
    @SerialName("last_prompt_tokens") val lastPromptTokens: Int? = null,
    @SerialName("input_tokens") val inputTokens: Int? = null,
    @SerialName("output_tokens") val outputTokens: Int? = null,
    @SerialName("estimated_cost") val estimatedCost: Double? = null
)
