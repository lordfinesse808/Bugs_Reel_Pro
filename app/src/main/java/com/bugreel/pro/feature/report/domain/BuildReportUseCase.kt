package com.bugreel.pro.feature.report.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import javax.inject.Inject

// CLAUDE.md Sprint 2 Session 2 — pure validation logic (no IO, no suspend), mirroring the
// "return every error, not just the first" spirit of Section 10's QualityScorer contract so
// ReportBuilderScreen can show every invalid field at once instead of one-at-a-time.
class BuildReportUseCase @Inject constructor() {

    operator fun invoke(draft: BugReportDraft): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        if (draft.title.isNullOrBlank()) {
            errors += ValidationError("title", "Title is required")
        }
        if (draft.summary.isNullOrBlank()) {
            errors += ValidationError("summary", "Description is required")
        }
        if (draft.severity == null) {
            errors += ValidationError("severity", "Severity is required")
        }
        if (draft.frequency == null) {
            errors += ValidationError("frequency", "Frequency is required")
        }
        if (draft.reproSteps.isNullOrEmpty()) {
            errors += ValidationError("reproSteps", "At least one repro step is required")
        }
        val evidence = draft.evidenceBundle
        if (evidence == null || (evidence.recordingUri == null && evidence.screenshots.isEmpty())) {
            errors += ValidationError("evidence", "Attach a recording or at least one screenshot")
        }
        return errors
    }

    // Returns null if the draft doesn't pass validation — callers must check invoke() first
    // (or check the return value) rather than treating a non-null result as a skip of validation.
    fun buildOrNull(draft: BugReportDraft): BugReport? {
        if (invoke(draft).isNotEmpty()) return null
        val deviceProfile = draft.deviceProfile ?: return null
        val evidenceBundle = draft.evidenceBundle ?: return null
        return BugReport(
            id = draft.id,
            title = draft.title!!,
            summary = draft.summary!!,
            reproSteps = draft.reproSteps!!,
            severity = draft.severity!!,
            frequency = draft.frequency!!,
            deviceProfile = deviceProfile,
            evidenceBundle = evidenceBundle,
            qualityScore = draft.qualityScore,
            aiOutput = draft.aiOutput,
            platform = draft.platform ?: TestPlatform.OTHER,
            status = ReportStatus.SUBMITTED,
            createdAt = draft.createdAt,
            updatedAt = Instant.now(),
            syncedAt = null,
            outcomeFeedback = null
        )
    }
}
