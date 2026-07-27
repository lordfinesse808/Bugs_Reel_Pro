package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Severity
import javax.inject.Inject

// CLAUDE.md Section 16 — pure tree logic recommending a starting Severity from the evidence a
// tester has attached so far. This is advice shown to the tester, not an enforced value --
// QualityScorer.scoreSeverity checks consistency against whatever severity the tester actually
// picks, independently of this recommendation.
class SeverityAdvisor @Inject constructor() {

    fun recommend(evidence: EvidenceBundle?): Severity {
        if (evidence == null) return Severity.COSMETIC

        val combinedLog = listOfNotNull(evidence.logText, evidence.eventLog)
            .joinToString(" ")
            .lowercase()
        val hasCrashSignal = CRASH_KEYWORDS.any { combinedLog.contains(it) }
        val hasRecording = !evidence.recordingUri.isNullOrBlank()
        val hasScreenshots = evidence.screenshots.isNotEmpty()
        val hasLogText = !evidence.logText.isNullOrBlank() || !evidence.eventLog.isNullOrBlank()

        return when {
            hasCrashSignal -> Severity.CRITICAL
            hasRecording && hasScreenshots -> Severity.HIGH
            hasRecording || hasScreenshots -> Severity.MEDIUM
            hasLogText -> Severity.LOW
            else -> Severity.COSMETIC
        }
    }

    companion object {
        private val CRASH_KEYWORDS = listOf(
            "crash", "fatal exception", "fatal signal", "anr", "force close", "not responding"
        )
    }
}
