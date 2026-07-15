package com.bugreel.pro.core.domain.model

data class AiOutput(
    val generatedTitle: String?,
    val generatedSummary: String?,
    val formattedReproSteps: List<String>,
    val safetyPassed: Boolean,
    val blockedReason: String?
)
