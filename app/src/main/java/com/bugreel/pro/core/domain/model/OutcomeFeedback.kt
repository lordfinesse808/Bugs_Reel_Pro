package com.bugreel.pro.core.domain.model

import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import java.time.Instant

// CLAUDE.md Section 1 / Section 6 / Section 23 — THE FLYWHEEL.
// Every field here feeds the training pipeline. All are required — never make any of these nullable.
data class OutcomeFeedback(
    val reportId: String,
    val platformOutcome: PlatformOutcome,
    val rejectionReason: String?,
    val reportedByTesterAt: Instant,
    val preAiQualityScore: Int,
    val postAiQualityScore: Int,
    val testerOverrodeSuggestion: Boolean,
    val aiSuggestionsAccepted: List<String>,
    val aiSuggestionsRejected: List<String>
)
