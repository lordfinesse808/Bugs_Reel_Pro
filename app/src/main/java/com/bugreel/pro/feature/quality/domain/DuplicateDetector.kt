package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import javax.inject.Inject

// CLAUDE.md Section 16 — similarity check against the user's existing reports. Pure Jaccard
// token overlap on title+summary; no IO of its own (the caller, ScoreReportUseCase, supplies
// the existing-report list via QualityRepository).
class DuplicateDetector @Inject constructor() {

    fun findDuplicates(
        candidate: BugReportDraft,
        existing: List<BugReport>,
        threshold: Double = DEFAULT_THRESHOLD
    ): List<BugReport> {
        val candidateTokens = tokenize(candidate.title, candidate.summary)
        if (candidateTokens.isEmpty()) return emptyList()

        return existing.filter { report ->
            val reportTokens = tokenize(report.title, report.summary)
            reportTokens.isNotEmpty() && jaccardSimilarity(candidateTokens, reportTokens) >= threshold
        }
    }

    private fun tokenize(title: String?, summary: String?): Set<String> =
        listOfNotNull(title, summary)
            .joinToString(" ")
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > MIN_TOKEN_LENGTH }
            .toSet()

    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        val union = a union b
        if (union.isEmpty()) return 0.0
        val intersection = a intersect b
        return intersection.size.toDouble() / union.size
    }

    companion object {
        const val DEFAULT_THRESHOLD = 0.6
        private const val MIN_TOKEN_LENGTH = 2
    }
}
