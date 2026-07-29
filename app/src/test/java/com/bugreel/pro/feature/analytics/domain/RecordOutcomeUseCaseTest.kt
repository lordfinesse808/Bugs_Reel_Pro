package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// CLAUDE.md Section 18 item 13 — submitting feedback correctly links to reportId. BuildReportUseCase
// (Sprint 3/4) already wrote this report's row at submission time with platformOutcome=PENDING;
// this is the tester actually resolving it, so the update must preserve everything else on the row
// (flywheel fields already captured) and only change outcome/rejectionReason/reportedByTesterAt.
class RecordOutcomeUseCaseTest {

    private val outcomeFeedbackRepository = mockk<OutcomeFeedbackRepository>()
    private val useCase = RecordOutcomeUseCase(outcomeFeedbackRepository)

    private fun pendingFeedback(reportId: String) = OutcomeFeedback(
        reportId = reportId,
        platformOutcome = PlatformOutcome.PENDING,
        rejectionReason = null,
        reportedByTesterAt = Instant.parse("2026-01-01T00:00:00Z"),
        preAiQualityScore = 55,
        postAiQualityScore = 82,
        testerOverrodeSuggestion = true,
        aiSuggestionsAccepted = listOf("title"),
        aiSuggestionsRejected = listOf("summary")
    )

    @Test
    fun `submitting ACCEPTED outcome links to the correct reportId and clears rejectionReason`() = runTest {
        val existing = pendingFeedback("report-1")
        coEvery { outcomeFeedbackRepository.queryByReportId("report-1") } returns flowOf(existing)
        val feedbackSlot = slot<OutcomeFeedback>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs

        useCase("report-1", PlatformOutcome.ACCEPTED, rejectionReason = null)

        val saved = feedbackSlot.captured
        assertEquals("report-1", saved.reportId)
        assertEquals(PlatformOutcome.ACCEPTED, saved.platformOutcome)
        assertEquals(null, saved.rejectionReason)
    }

    @Test
    fun `submitting REJECTED outcome records the rejection reason`() = runTest {
        val existing = pendingFeedback("report-2")
        coEvery { outcomeFeedbackRepository.queryByReportId("report-2") } returns flowOf(existing)
        val feedbackSlot = slot<OutcomeFeedback>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs

        useCase("report-2", PlatformOutcome.REJECTED, rejectionReason = "Not reproducible")

        val saved = feedbackSlot.captured
        assertEquals(PlatformOutcome.REJECTED, saved.platformOutcome)
        assertEquals("Not reproducible", saved.rejectionReason)
    }

    @Test
    fun `preserves preAiQualityScore, postAiQualityScore, and AI suggestion bookkeeping already on the row`() = runTest {
        val existing = pendingFeedback("report-3")
        coEvery { outcomeFeedbackRepository.queryByReportId("report-3") } returns flowOf(existing)
        val feedbackSlot = slot<OutcomeFeedback>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs

        useCase("report-3", PlatformOutcome.ACCEPTED, rejectionReason = null)

        val saved = feedbackSlot.captured
        assertEquals(existing.preAiQualityScore, saved.preAiQualityScore)
        assertEquals(existing.postAiQualityScore, saved.postAiQualityScore)
        assertEquals(existing.testerOverrodeSuggestion, saved.testerOverrodeSuggestion)
        assertEquals(existing.aiSuggestionsAccepted, saved.aiSuggestionsAccepted)
        assertEquals(existing.aiSuggestionsRejected, saved.aiSuggestionsRejected)
    }

    @Test
    fun `throws when no OutcomeFeedback row exists for the reportId`() = runTest {
        coEvery { outcomeFeedbackRepository.queryByReportId("missing-report") } returns flowOf(null)

        val thrown = runCatching {
            useCase("missing-report", PlatformOutcome.ACCEPTED, rejectionReason = null)
        }.exceptionOrNull()

        assertNotNull(thrown)
        assertEquals(IllegalStateException::class, thrown!!::class)
    }
}
