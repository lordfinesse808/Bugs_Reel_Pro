package com.bugreel.pro.core.data.local.onboarding

import kotlinx.serialization.Serializable

// CLAUDE.md Section 13 Track 3 / Section 18 — tester consent and the AI training opt-in are
// two SEPARATE fields with separate confirmation flows (Section 18 key implementation note:
// "never bundled"), even though they're persisted in the same small preferences object.
@Serializable
data class ConsentPreferences(
    val consentGiven: Boolean = false,
    val aiTrainingOptIn: Boolean = false
)
