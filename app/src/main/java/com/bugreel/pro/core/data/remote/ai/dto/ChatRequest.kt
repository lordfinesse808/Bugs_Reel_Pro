package com.bugreel.pro.core.data.remote.ai.dto

import kotlinx.serialization.Serializable

// CLAUDE.md Section 9 / Section 17 — OpenAI Chat Completions request body (text-only). Used for
// Title, Summary, ReproSteps (BuildConfig.AI_MODEL_STANDARD) and SafetyValidator
// (BuildConfig.AI_MODEL_SAFETY) — model routing happens at the call site, never hardcoded here.
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = DEFAULT_TEMPERATURE
) {
    companion object {
        private const val DEFAULT_TEMPERATURE = 0.2
    }
}

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
