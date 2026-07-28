package com.bugreel.pro.feature.report.domain

import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.core.domain.quality.QualityScorer
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// CLAUDE.md Section 16 item 10 / Section 23 — verifies the flywheel's preAiQualityScore starts
// accumulating at submission, and Sprint 4's postAiQualityScore/testerOverrodeSuggestion/
// aiSuggestions* wiring reads correctly from AiOutput when AI Assist was used.
class BuildReportUseCaseTest {

    private val outcomeFeedbackRepository = mockk<OutcomeFeedbackRepository>()
    private val useCase = BuildReportUseCase(
        qualityScorer = QualityScorer(),
        outcomeFeedbackRepository = outcomeFeedbackRepository
    )

    @Test
    fun `buildOrNull attaches a real quality score`() {
        val report = useCase.buildOrNull(completeDraft())

        assertNotNull(report)
        assertNotNull(report!!.qualityScore)
        assertEquals(25, report.qualityScore!!.reproduction)
    }

    @Test
    fun `recordOutcomeFeedback without AI assist records the draft score as pre-AI with no post-AI score`() = runTest {
        val feedbackSlot = slot<OutcomeFeedback>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs
        val report = useCase.buildOrNull(completeDraft())!!

        useCase.recordOutcomeFeedback(report)

        val feedback = feedbackSlot.captured
        assertEquals(report.id, feedback.reportId)
        assertEquals(PlatformOutcome.PENDING, feedback.platformOutcome)
        assertEquals(report.qualityScore!!.total, feedback.preAiQualityScore)
        assertEquals(0, feedback.postAiQualityScore)
        assertFalse(feedback.testerOverrodeSuggestion)
        assertEquals(emptyList<String>(), feedback.aiSuggestionsAccepted)
        assertEquals(emptyList<String>(), feedback.aiSuggestionsRejected)
    }

    @Test
    fun `recordOutcomeFeedback with AI assist reads pre-AI score and override bookkeeping from AiOutput`() = runTest {
        val feedbackSlot = slot<OutcomeFeedback>()
        coEvery { outcomeFeedbackRepository.insert(capture(feedbackSlot)) } just Runs
        val draft = completeDraft().copy(
            aiOutput = AiOutput(
                generatedTitle = "AI generated title",
                generatedSummary = "AI generated summary",
                formattedReproSteps = listOf("Step A", "Step B"),
                safetyPassed = true,
                blockedReason = null,
                preAiQualityScore = 40,
                testerOverrodeSuggestion = true,
                aiSuggestionsAccepted = listOf("title"),
                aiSuggestionsRejected = listOf("summary")
            )
        )
        val report = useCase.buildOrNull(draft)!!

        useCase.recordOutcomeFeedback(report)

        val feedback = feedbackSlot.captured
        assertEquals(40, feedback.preAiQualityScore)
        assertEquals(report.qualityScore!!.total, feedback.postAiQualityScore)
        assertTrue(feedback.testerOverrodeSuggestion)
        assertEquals(listOf("title"), feedback.aiSuggestionsAccepted)
        assertEquals(listOf("summary"), feedback.aiSuggestionsRejected)
    }

    private fun completeDraft() = BugReportDraft(
        id = "draft-1",
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after a cold start of the app.",
        reproSteps = listOf("Cold start the app", "Tap the login button"),
        severity = Severity.HIGH,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(
            recordingUri = "content://media/recording.mp4",
            screenshots = listOf("content://media/screenshot_0.png"),
            logText = "some log output",
            eventLog = null
        ),
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.UTEST,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
