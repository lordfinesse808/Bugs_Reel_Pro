package com.bugreel.pro.core.data.remote.ai.dto

import kotlinx.serialization.Serializable

// CLAUDE.md Section 9 / Section 17 — OpenAI Chat Completions response body. Only the fields
// OpenAIService actually reads are modelled; kotlinx.serialization ignores unknown JSON keys by
// default (Json { ignoreUnknownKeys = true } is set where this is decoded), so the real
// response's extra fields (usage, id, ...) never need representing here.
@Serializable
data class ChatResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    val message: ChatResponseMessage
)

@Serializable
data class ChatResponseMessage(
    val content: String? = null
)
