package com.bugreel.pro.core.data.remote.ai

// CLAUDE.md Section 9 — the AI rate limiter and network layer need to distinguish a clean
// failure from a rate-limit hit (which carries its own retry-after countdown for the UI).
sealed class AIResult<out T> {
    data class Success<T>(val value: T) : AIResult<T>()
    data class Error(val message: String) : AIResult<Nothing>()
    data class RateLimitExceeded(val retryAfterSeconds: Int) : AIResult<Nothing>()
}
