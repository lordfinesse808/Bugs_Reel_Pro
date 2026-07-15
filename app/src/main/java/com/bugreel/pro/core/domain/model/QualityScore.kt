package com.bugreel.pro.core.domain.model

// CLAUDE.md Section 6 / Section 10 — produced by the pure QualityScorer function (Sprint 3).
data class QualityScore(
    val total: Int,
    val reproduction: Int,
    val evidence: Int,
    val description: Int,
    val severity: Int,
    val metadata: Int,
    val coaching: List<CoachingHint>
)
