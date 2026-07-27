package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import com.bugreel.pro.core.domain.repository.ReportRepository
import javax.inject.Inject

// CLAUDE.md Section 16 item 10 / Section 23 — THE FLYWHEEL starts accumulating
// preAiQualityScore here. This does the job Section 16 originally sketched for
// feature/report/domain/BuildReportUseCase.kt, but lives in feature/quality instead:
// QualityScorer is feature-scoped (feature/quality/domain), and Architecture Rule 4 forbids
// feature/report from importing another feature package. Scoring a freshly submitted report
// from within feature/quality -- triggered when QualityScoreScreen first loads it -- keeps
// that boundary intact. See CLAUDE.md Section 16 amendment note.
class RecordInitialQualityScoreUseCase @Inject constructor(
    private val qualityScorer: QualityScorer,
    private val reportRepository: ReportRepository,
    private val outcomeFeedbackRepository: OutcomeFeedbackRepository
) {
    suspend operator fun invoke(report: BugReport): BugReport {
        if (report.qualityScore != null) return report

        val score = qualityScorer.score(report.toDraft())
        val scoredReport = report.copy(qualityScore = score)
        reportRepository.updateReport(scoredReport)
        outcomeFeedbackRepository.insert(
            OutcomeFeedback(
                reportId = report.id,
                platformOutcome = PlatformOutcome.PENDING,
                rejectionReason = null,
                reportedByTesterAt = report.createdAt,
                preAiQualityScore = score.total,
                postAiQualityScore = 0,
                testerOverrodeSuggestion = false,
                aiSuggestionsAccepted = emptyList(),
                aiSuggestionsRejected = emptyList()
            )
        )
        return scoredReport
    }

    // A submitted BugReport has already passed BuildReportUseCase's required-field validation,
    // so this mapping only ever produces "filled" values for those fields -- QualityScorer's
    // null-handling branches exist for the pure-function contract (Section 10) and its own
    // direct unit tests, not because a real submitted report can be missing them.
    private fun BugReport.toDraft() = BugReportDraft(
        id = id,
        title = title,
        summary = summary,
        reproSteps = reproSteps,
        severity = severity,
        frequency = frequency,
        deviceProfile = deviceProfile,
        evidenceBundle = evidenceBundle,
        qualityScore = qualityScore,
        aiOutput = aiOutput,
        platform = platform,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
