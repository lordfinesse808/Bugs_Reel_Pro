package com.bugreel.pro.core.domain.model

// CLAUDE.md Section 6 — message text is sourced from CoachingHintStrings.kt (Sprint 3), max 120 chars.
data class CoachingHint(
    val field: String,
    val message: String,
    val scoreImpact: Int
)
