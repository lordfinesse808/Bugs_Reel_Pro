package com.bugreel.pro.core.domain.model

// CLAUDE.md Section 9 — everything PromptRepository and AIService need to ground generated
// text in real evidence (SafetyValidator's four criteria all check output against these
// fields). deviceProfile/evidenceBundle are non-null here even though they're optional on
// BugReportDraft: AI assist only runs once capture has completed and both are known.
data class ReportContext(
    val draft: BugReportDraft,
    val deviceProfile: DeviceProfile,
    val evidenceBundle: EvidenceBundle
)
