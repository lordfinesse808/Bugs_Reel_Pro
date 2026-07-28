package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.data.remote.ai.AIResult
import javax.inject.Inject
import javax.inject.Singleton

// CLAUDE.md Section 9 — soft, per-user, sliding-window limiter over BuildConfig.AI_RATE_LIMIT_RPM
// requests/minute. Takes an explicit timestamp so tests are deterministic (no wall-clock
// dependency); callers pass System.currentTimeMillis() in production.
@Singleton
class AiRateLimiter @Inject constructor() {

    private val requestTimestampsMillis = ArrayDeque<Long>()

    @Synchronized
    fun tryAcquire(nowMillis: Long): AIResult<Unit> {
        pruneOutsideWindow(nowMillis)
        if (requestTimestampsMillis.size >= BuildConfig.AI_RATE_LIMIT_RPM) {
            val oldest = requestTimestampsMillis.first()
            val retryAfterSeconds = ((oldest + WINDOW_MILLIS - nowMillis) / MILLIS_PER_SECOND)
                .toInt()
                .coerceAtLeast(1)
            return AIResult.RateLimitExceeded(retryAfterSeconds)
        }
        requestTimestampsMillis.addLast(nowMillis)
        return AIResult.Success(Unit)
    }

    private fun pruneOutsideWindow(nowMillis: Long) {
        while (requestTimestampsMillis.isNotEmpty() && nowMillis - requestTimestampsMillis.first() >= WINDOW_MILLIS) {
            requestTimestampsMillis.removeFirst()
        }
    }

    private companion object {
        const val WINDOW_MILLIS = 60_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
