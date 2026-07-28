package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.core.domain.quality.QualityScorer
import io.mockk.coEvery
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScoreReportUseCaseTest {

    private val qualityRepository = mockk<QualityRepository>()
    private val useCase = ScoreReportUseCase(
        qualityScorer = QualityScorer(),
        severityAdvisor = SeverityAdvisor(),
        completenessChecker = CompletenessChecker(),
        duplicateDetector = DuplicateDetector(),
        qualityRepository = qualityRepository
    )

    @Test
    fun `assembles score, recommended severity, missing fields, and duplicates`() = runTest {
        val draft = draft(title = "Crash on launch", summary = "The app crashes immediately every time it is opened.")
        coEvery { qualityRepository.getExistingReports(excludingId = draft.id) } returns
            listOf(report(id = "existing-1", title = "Crash on launch", summary = "The app crashes immediately every time it is opened."))

        val assessment = useCase(draft)

        assertEquals(Severity.COSMETIC, assessment.recommendedSeverity) // no evidence attached
        assertTrue(assessment.missingFields.contains("reproSteps"))
        assertEquals(1, assessment.duplicates.size)
        assertEquals("existing-1", assessment.duplicates.first().id)
        assertEquals(0, assessment.qualityScore.evidence)
    }

    @Test
    fun `excludes the draft's own id when looking up existing reports`() = runTest {
        val draft = draft(title = "Crash on launch", summary = "The app crashes immediately every time it is opened.")
        coEvery { qualityRepository.getExistingReports(excludingId = draft.id) } returns emptyList()

        val assessment = useCase(draft)

        assertTrue(assessment.duplicates.isEmpty())
    }

    private fun draft(title: String?, summary: String?) = BugReportDraft(
        id = "draft-1",
        title = title,
        summary = summary,
        reproSteps = null,
        severity = null,
        frequency = Frequency.SOMETIMES,
        deviceProfile = null,
        evidenceBundle = null,
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.OTHER,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = null
    )

    private fun report(id: String, title: String, summary: String) = BugReport(
        id = id,
        title = title,
        summary = summary,
        reproSteps = listOf("Step one", "Step two"),
        severity = Severity.MEDIUM,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(null, emptyList(), null, null),
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
