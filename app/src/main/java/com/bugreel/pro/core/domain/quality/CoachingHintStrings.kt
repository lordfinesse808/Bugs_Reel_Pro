package com.bugreel.pro.core.domain.quality

// CLAUDE.md Section 10 / Section 16 — every coaching hint message QualityScorer can emit lives
// here, and nowhere else. All entries must stay <=120 chars (enforced by
// CoachingHintStringsTest); ALL exists so that test can iterate without reflection.
object CoachingHintStrings {
    const val REPRODUCTION =
        "Add at least 2 clear repro steps (10+ characters each) so testers can reproduce this bug."
    const val EVIDENCE =
        "Attach a screen recording and screenshots -- visual evidence raises your score the most."
    const val DESCRIPTION =
        "Write a clear summary (50-500 characters) describing what went wrong, no placeholder text."
    const val SEVERITY =
        "Pick a severity that matches your evidence -- Critical/High need a recording or log."
    const val METADATA =
        "Fill in your full device profile and select a test platform to complete your metadata."

    val ALL: List<String> = listOf(REPRODUCTION, EVIDENCE, DESCRIPTION, SEVERITY, METADATA)
}
