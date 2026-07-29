package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.quality.QualityScorer
import javax.inject.Inject

// CLAUDE.md Section 18 item 3 / key implementation notes — "should feel like a game -- immediate
// score, improve this retry loop, not a tutorial." A scenario gives the tester something concrete
// to describe; scoring reuses the same deterministic QualityScorer real submissions get, so
// practice mode teaches the actual rubric rather than a simplified stand-in for it.
data class PracticeScenario(
    val id: String,
    val prompt: String,
    val hint: String
)

class PracticeModeUseCase @Inject constructor(
    private val qualityScorer: QualityScorer
) {
    fun scenarios(): List<PracticeScenario> = SCENARIOS

    fun score(draft: BugReportDraft): QualityScore = qualityScorer.score(draft)

    private companion object {
        val SCENARIOS = listOf(
            PracticeScenario(
                id = "login-crash",
                prompt = "The login screen crashes when you rotate the device mid-animation.",
                hint = "Try including exact rotation timing, device orientation, and a crash log excerpt."
            ),
            PracticeScenario(
                id = "checkout-double-charge",
                prompt = "Tapping \"Pay\" twice quickly on the checkout screen charges the card twice.",
                hint = "Note the tap interval, network conditions, and whether a second confirmation appeared."
            ),
            PracticeScenario(
                id = "dark-mode-contrast",
                prompt = "Body text is unreadable against the background in dark mode on the settings screen.",
                hint = "Describe the exact screen, color values if visible, and attach a screenshot."
            )
        )
    }
}
