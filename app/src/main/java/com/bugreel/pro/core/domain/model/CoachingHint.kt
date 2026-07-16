package com.bugreel.pro.core.domain.model

import kotlinx.serialization.Serializable

// CLAUDE.md Section 6 — message text is sourced from CoachingHintStrings.kt (Sprint 3), max 120 chars.
// @Serializable (a plain Kotlin annotation, not an Android import) so QualityScoreEntity.coaching
// can round-trip as a JSON array at the ReportRepositoryImpl mapping layer.
@Serializable
data class CoachingHint(
    val field: String,
    val message: String,
    val scoreImpact: Int
)
