package com.bugreel.pro.core.data.remote.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// CLAUDE.md Section 9 / Section 17 — vision-endpoint request, used only by SafetyValidator when
// the evidence bundle has screenshots to ground its check against (CLAUDE.md Section 9 / Section
// 17 key implementation notes: vision calls cost more, model separately, only when screenshots
// are present). Always routed through BuildConfig.AI_MODEL_SAFETY.
@Serializable
data class VisionRequest(
    val model: String,
    val messages: List<VisionMessage>,
    val temperature: Double = DEFAULT_TEMPERATURE
) {
    companion object {
        private const val DEFAULT_TEMPERATURE = 0.0
    }
}

@Serializable
data class VisionMessage(
    val role: String,
    val content: List<VisionContentPart>
)

@Serializable
data class VisionContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: VisionImageUrl? = null
)

@Serializable
data class VisionImageUrl(
    val url: String
)
