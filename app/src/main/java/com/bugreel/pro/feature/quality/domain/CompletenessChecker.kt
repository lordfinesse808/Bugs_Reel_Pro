package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReportDraft
import javax.inject.Inject

// CLAUDE.md Section 16 — reports which required fields are still missing from a draft, each
// checked independently so the UI can list every gap at once rather than one-at-a-time.
class CompletenessChecker @Inject constructor() {

    fun missingFields(draft: BugReportDraft): List<String> {
        val missing = mutableListOf<String>()
        if (draft.title.isNullOrBlank()) missing += "title"
        if (draft.summary.isNullOrBlank()) missing += "summary"
        if (draft.reproSteps.isNullOrEmpty()) missing += "reproSteps"
        if (draft.severity == null) missing += "severity"
        if (draft.frequency == null) missing += "frequency"
        if (draft.deviceProfile == null) missing += "deviceProfile"
        if (isEvidenceMissing(draft)) missing += "evidence"
        if (draft.platform == null) missing += "platform"
        return missing
    }

    fun isComplete(draft: BugReportDraft): Boolean = missingFields(draft).isEmpty()

    private fun isEvidenceMissing(draft: BugReportDraft): Boolean {
        val evidence = draft.evidenceBundle ?: return true
        return evidence.recordingUri.isNullOrBlank() && evidence.screenshots.isEmpty()
    }
}
