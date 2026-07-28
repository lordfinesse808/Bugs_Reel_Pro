package com.bugreel.pro.core.domain.quality

import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.CoachingHint
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import javax.inject.Inject

// CLAUDE.md Section 10 — pure function. Zero IO, zero coroutines, zero Android imports, zero
// AI calls. Same input always produces the same output; scored against a BugReportDraft (not
// the non-nullable BugReport) so this can run live while the tester is still filling the form.
//
// Lives in core/domain (not feature/quality/domain as originally sketched in CLAUDE.md Section
// 16) because Sprint 4 needs it callable from feature/ai too (to score a pre-AI-suggestion
// baseline), and Architecture Rule 4 forbids one feature package importing another. See
// CLAUDE.md Section 16 amendment (2026-07-28).
class QualityScorer @Inject constructor() {

    fun score(draft: BugReportDraft): QualityScore {
        val reproduction = scoreReproduction(draft.reproSteps)
        val evidence = scoreEvidence(draft.evidenceBundle)
        val description = scoreDescription(draft.summary)
        val severity = scoreSeverity(draft.severity, draft.evidenceBundle)
        val metadata = scoreMetadata(draft.deviceProfile, draft.platform)

        val dimensions = listOf(
            Dimension("reproduction", reproduction, MAX_REPRODUCTION, CoachingHintStrings.REPRODUCTION),
            Dimension("evidence", evidence, MAX_EVIDENCE, CoachingHintStrings.EVIDENCE),
            Dimension("description", description, MAX_DESCRIPTION, CoachingHintStrings.DESCRIPTION),
            Dimension("severity", severity, MAX_SEVERITY, CoachingHintStrings.SEVERITY),
            Dimension("metadata", metadata, MAX_METADATA, CoachingHintStrings.METADATA)
        )

        val coaching = dimensions
            .filter { it.score < it.max * COACHING_THRESHOLD }
            .sortedByDescending { it.max - it.score }
            .take(MAX_COACHING_HINTS)
            .map { CoachingHint(field = it.field, message = it.message, scoreImpact = it.max - it.score) }

        return QualityScore(
            total = reproduction + evidence + description + severity + metadata,
            reproduction = reproduction,
            evidence = evidence,
            description = description,
            severity = severity,
            metadata = metadata,
            coaching = coaching
        )
    }

    private fun scoreReproduction(steps: List<String>?): Int {
        val nonBlank = steps.orEmpty().filter { it.isNotBlank() }
        if (nonBlank.isEmpty()) return 0
        var score = REPRODUCTION_PRESENT_POINTS
        if (nonBlank.size >= MIN_STEP_COUNT) score += REPRODUCTION_COUNT_POINTS
        if (nonBlank.all { it.trim().length >= MIN_CLEAR_STEP_LENGTH }) score += REPRODUCTION_CLARITY_POINTS
        return score
    }

    private fun scoreEvidence(evidence: EvidenceBundle?): Int {
        if (evidence == null) return 0
        var score = 0
        if (!evidence.recordingUri.isNullOrBlank()) score += EVIDENCE_RECORDING_POINTS
        if (evidence.screenshots.isNotEmpty()) score += EVIDENCE_SCREENSHOTS_POINTS
        if (hasLogEvidence(evidence)) score += EVIDENCE_LOGS_POINTS
        return score
    }

    private fun scoreDescription(summary: String?): Int {
        if (summary.isNullOrBlank()) return 0
        var score = 0
        if (summary.length > MIN_SUMMARY_LENGTH) score += DESCRIPTION_LENGTH_MIN_POINTS
        if (summary.length < MAX_SUMMARY_LENGTH) score += DESCRIPTION_LENGTH_MAX_POINTS
        if (!isPlaceholderText(summary)) score += DESCRIPTION_NO_PLACEHOLDER_POINTS
        return score
    }

    private fun scoreSeverity(severity: Severity?, evidence: EvidenceBundle?): Int {
        if (severity == null) return 0
        val hasStrongEvidence = evidence != null &&
            (!evidence.recordingUri.isNullOrBlank() || hasLogEvidence(evidence))
        val consistent = when (severity) {
            Severity.CRITICAL, Severity.HIGH -> hasStrongEvidence
            Severity.MEDIUM, Severity.LOW, Severity.COSMETIC -> true
        }
        return SEVERITY_FILLED_POINTS + if (consistent) SEVERITY_CONSISTENT_POINTS else 0
    }

    private fun scoreMetadata(deviceProfile: DeviceProfile?, platform: TestPlatform?): Int {
        var score = 0
        if (isDeviceProfileComplete(deviceProfile)) score += METADATA_DEVICE_PROFILE_POINTS
        if (platform != null) score += METADATA_PLATFORM_POINTS
        return score
    }

    private fun hasLogEvidence(evidence: EvidenceBundle): Boolean =
        !evidence.logText.isNullOrBlank() || !evidence.eventLog.isNullOrBlank()

    private fun isDeviceProfileComplete(profile: DeviceProfile?): Boolean {
        if (profile == null) return false
        return profile.brand.isNotBlank() &&
            profile.model.isNotBlank() &&
            profile.osVersion.isNotBlank() &&
            profile.ramMb > 0 &&
            profile.screenResolution.isNotBlank() &&
            profile.networkType.isNotBlank()
    }

    // Exact-match (not substring) on purpose: a real summary that happens to mention "test" or
    // "testing" -- unavoidable in a crowdtesting app -- must never be penalized as a placeholder.
    // The word-repetition check separately catches gaming attempts like "test test test test".
    private fun isPlaceholderText(text: String): Boolean {
        val normalized = text.trim().lowercase()
        if (normalized in PLACEHOLDER_PHRASES) return true
        val words = normalized.split(Regex("\\W+")).filter { it.isNotBlank() }
        return words.size > 1 && words.toSet().size == 1
    }

    private data class Dimension(val field: String, val score: Int, val max: Int, val message: String)

    companion object {
        private const val MAX_REPRODUCTION = 25
        private const val MAX_EVIDENCE = 25
        private const val MAX_DESCRIPTION = 20
        private const val MAX_SEVERITY = 15
        private const val MAX_METADATA = 15

        private const val REPRODUCTION_PRESENT_POINTS = 10
        private const val REPRODUCTION_COUNT_POINTS = 10
        private const val REPRODUCTION_CLARITY_POINTS = 5
        private const val MIN_STEP_COUNT = 2
        private const val MIN_CLEAR_STEP_LENGTH = 10

        private const val EVIDENCE_RECORDING_POINTS = 15
        private const val EVIDENCE_SCREENSHOTS_POINTS = 7
        private const val EVIDENCE_LOGS_POINTS = 3

        private const val DESCRIPTION_LENGTH_MIN_POINTS = 10
        private const val DESCRIPTION_LENGTH_MAX_POINTS = 5
        private const val DESCRIPTION_NO_PLACEHOLDER_POINTS = 5
        private const val MIN_SUMMARY_LENGTH = 50
        private const val MAX_SUMMARY_LENGTH = 500

        private const val SEVERITY_FILLED_POINTS = 10
        private const val SEVERITY_CONSISTENT_POINTS = 5

        private const val METADATA_DEVICE_PROFILE_POINTS = 10
        private const val METADATA_PLATFORM_POINTS = 5

        private const val COACHING_THRESHOLD = 0.8
        private const val MAX_COACHING_HINTS = 3

        private val PLACEHOLDER_PHRASES = listOf(
            "todo", "tbd", "n/a", "na", "test", "asdf", "placeholder", "xxx", "fixme"
        )
    }
}
