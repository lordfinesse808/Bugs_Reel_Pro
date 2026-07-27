package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import com.bugreel.pro.core.domain.repository.ReportRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

// CLAUDE.md Section 16 item 10 / Section 23 — verifies the flywheel actually starts
// accumulating preAiQualityScore in Sprint 3.
class RecordInitialQualityScoreUseCaseTest {

    private val reportRepository = mockk<ReportRepository>(relaxed = true)
    private val outcomeFeedbackRepository = mockk<OutcomeFeedbackRepository>(relaxed = true)
    private val useCase = RecordInitialQualityScoreUseCase(
        qualityScorer = QualityScorer(),
        reportRepository = reportRepository,
        outcomeFeedbackRepository = outcomeFeedbackRepository
    )

    @Test
    fun `scores the report and persists preAiQualityScore as pending`() = runTest {
        val report = testReport()
        val feedbackSlot = slot<OutcomeFeedback>()
        val updatedReportSlot = slot<BugReport>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs
        coEvery { reportRepository.updateReport(capture(updatedReportSlot)) } just Runs

        val result = useCase(report)

        assertNotNull(result.qualityScore)
        assertEquals(report.id, feedbackSlot.captured.reportId)
        assertEquals(PlatformOutcome.PENDING, feedbackSlot.captured.platformOutcome)
        assertEquals(result.qualityScore!!.total, feedbackSlot.captured.preAiQualityScore)
        assertEquals(0, feedbackSlot.captured.postAiQualityScore)
        assertNotNull(updatedReportSlot.captured.qualityScore)
    }

    @Test
    fun `does nothing when the report already has a quality score`() = runTest {
        val alreadyScored = testReport().copy(
            qualityScore = QualityScore(
                total = 50,
                reproduction = 10,
                evidence = 10,
                description = 10,
                severity = 10,
                metadata = 10,
                coaching = emptyList()
            )
        )

        val result = useCase(alreadyScored)

        assertEquals(alreadyScored, result)
        coVerify(exactly = 0) { outcomeFeedbackRepository.insert(any()) }
        coVerify(exactly = 0) { reportRepository.updateReport(any()) }
    }

    private fun testReport() = BugReport(
        id = "report-1",
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after a cold start.",
        reproSteps = listOf("Cold start the app", "Tap the login button"),
        severity = Severity.HIGH,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(
            recordingUri = "content://media/recording.mp4",
            screenshots = listOf("content://media/screenshot_0.png"),
            logText = null,
            eventLog = null
        ),
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.UTEST,
        status = ReportStatus.SUBMITTED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        syncedAt = null,
        outcomeFeedback = null
    )
}
