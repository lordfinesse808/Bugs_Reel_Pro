package com.bugreel.pro.core.data.remote.ai

// CLAUDE.md Section 9 / ADR-004 — SafetyValidator's only two outcomes. On Blocked, callers
// must fall back to the tester's original input and never display the flagged output.
sealed class SafetyResult {
    data object Pass : SafetyResult()
    data class Blocked(val reason: String) : SafetyResult()
}
