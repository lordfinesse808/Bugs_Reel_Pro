package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.quality.QualityScorer
import javax.inject.Inject

// The full picture ScoreReportUseCase assembles for the report-builder UI: the deterministic
// score plus the advisory bits that need IO (existing reports for duplicate detection) and
// therefore can't live inside the pure QualityScorer.
data class QualityAssessment(
    val qualityScore: QualityScore,
    val recommendedSeverity: Severity,
    val missingFields: List<String>,
    val duplicates: List<BugReport>
)

class ScoreReportUseCase @Inject constructor(
    private val qualityScorer: QualityScorer,
    private val severityAdvisor: SeverityAdvisor,
    private val completenessChecker: CompletenessChecker,
    private val duplicateDetector: DuplicateDetector,
    private val qualityRepository: QualityRepository
) {
    suspend operator fun invoke(draft: BugReportDraft): QualityAssessment {
        val existingReports = qualityRepository.getExistingReports(excludingId = draft.id)
        return QualityAssessment(
            qualityScore = qualityScorer.score(draft),
            recommendedSeverity = severityAdvisor.recommend(draft.evidenceBundle),
            missingFields = completenessChecker.missingFields(draft),
            duplicates = duplicateDetector.findDuplicates(draft, existingReports)
        )
    }
}
