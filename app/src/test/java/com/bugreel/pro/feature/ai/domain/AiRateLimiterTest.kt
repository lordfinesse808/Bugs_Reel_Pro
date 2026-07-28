package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.data.remote.ai.AIResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// CLAUDE.md Section 17 — "21 requests in 60s -> request 21 returns RateLimitExceeded".
// BuildConfig.AI_RATE_LIMIT_RPM is 20, so this test issues AI_RATE_LIMIT_RPM + 1 requests.
class AiRateLimiterTest {

    private val rateLimiter = AiRateLimiter()

    @Test
    fun `requests up to the limit within one window all succeed`() {
        repeat(BuildConfig.AI_RATE_LIMIT_RPM) { index ->
            val result = rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + index)
            assertTrue(result is AIResult.Success, "request ${index + 1} should have succeeded")
        }
    }

    @Test
    fun `the request after the limit within one window is rate limited`() {
        repeat(BuildConfig.AI_RATE_LIMIT_RPM) { index ->
            rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + index)
        }

        val result = rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + BuildConfig.AI_RATE_LIMIT_RPM)

        assertTrue(result is AIResult.RateLimitExceeded)
        assertTrue((result as AIResult.RateLimitExceeded).retryAfterSeconds > 0)
    }

    @Test
    fun `a request outside the 60 second window succeeds again`() {
        repeat(BuildConfig.AI_RATE_LIMIT_RPM) { index ->
            rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + index)
        }

        val result = rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + WINDOW_MILLIS + 1)

        assertTrue(result is AIResult.Success)
    }

    @Test
    fun `retryAfterSeconds counts down toward the window boundary`() {
        rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS)
        repeat(BuildConfig.AI_RATE_LIMIT_RPM - 1) {
            rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS)
        }

        val result = rateLimiter.tryAcquire(nowMillis = BASE_TIME_MILLIS + 40_000L) as AIResult.RateLimitExceeded

        assertEquals(20, result.retryAfterSeconds)
    }

    private companion object {
        const val BASE_TIME_MILLIS = 1_000_000_000L
        const val WINDOW_MILLIS = 60_000L
    }
}
