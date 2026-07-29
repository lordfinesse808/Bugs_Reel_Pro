package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

// CLAUDE.md Section 18 item 14 — rate computed correctly from seeded OutcomeFeedback data.
class AcceptanceRateUseCaseTest {

    private val analyticsRepository = mockk<AnalyticsRepository>()
    private val useCase = AcceptanceRateUseCase(analyticsRepository)

    private fun feedback(reportId: String, outcome: PlatformOutcome) = OutcomeFeedback(
        reportId = reportId,
        platformOutcome = outcome,
        rejectionReason = if (outcome == PlatformOutcome.REJECTED) "Not reproducible" else null,
        reportedByTesterAt = Instant.parse("2026-01-01T00:00:00Z"),
        preAiQualityScore = 50,
        postAiQualityScore = 75,
        testerOverrodeSuggestion = false,
        aiSuggestionsAccepted = emptyList(),
        aiSuggestionsRejected = emptyList()
    )

    @Test
    fun `no OutcomeFeedback rows yields null rate and all-zero counts`() = runTest {
        coEvery { analyticsRepository.getAllOutcomeFeedback() } returns flowOf(emptyList())

        val result = useCase()

        assertEquals(0, result.accepted)
        assertEquals(0, result.rejected)
        assertEquals(0, result.pending)
        assertNull(result.rate)
    }

    @Test
    fun `only PENDING rows yields null rate, not zero`() = runTest {
        coEvery { analyticsRepository.getAllOutcomeFeedback() } returns flowOf(
            listOf(
                feedback("r1", PlatformOutcome.PENDING),
                feedback("r2", PlatformOutcome.PENDING)
            )
        )

        val result = useCase()

        assertEquals(2, result.pending)
        assertNull(result.rate)
    }

    @Test
    fun `3 accepted and 1 rejected out of 4 resolved yields 75 percent rate`() = runTest {
        coEvery { analyticsRepository.getAllOutcomeFeedback() } returns flowOf(
            listOf(
                feedback("r1", PlatformOutcome.ACCEPTED),
                feedback("r2", PlatformOutcome.ACCEPTED),
                feedback("r3", PlatformOutcome.ACCEPTED),
                feedback("r4", PlatformOutcome.REJECTED)
            )
        )

        val result = useCase()

        assertEquals(3, result.accepted)
        assertEquals(1, result.rejected)
        assertEquals(0.75, result.rate)
    }

    @Test
    fun `pending rows are excluded from the resolved denominator`() = runTest {
        coEvery { analyticsRepository.getAllOutcomeFeedback() } returns flowOf(
            listOf(
                feedback("r1", PlatformOutcome.ACCEPTED),
                feedback("r2", PlatformOutcome.REJECTED),
                feedback("r3", PlatformOutcome.PENDING)
            )
        )

        val result = useCase()

        assertEquals(1, result.pending)
        assertEquals(0.5, result.rate)
    }

    @Test
    fun `all rejected yields a 0 percent rate, distinct from no data`() = runTest {
        coEvery { analyticsRepository.getAllOutcomeFeedback() } returns flowOf(
            listOf(
                feedback("r1", PlatformOutcome.REJECTED),
                feedback("r2", PlatformOutcome.REJECTED)
            )
        )

        val result = useCase()

        assertEquals(0.0, result.rate)
    }
}
