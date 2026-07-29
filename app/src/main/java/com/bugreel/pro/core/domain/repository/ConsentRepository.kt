package com.bugreel.pro.core.domain.repository

import com.bugreel.pro.core.domain.model.ConsentState
import kotlinx.coroutines.flow.Flow

// CLAUDE.md Section 18 — gates app entry (AppNavGraph) before any capture or report activity.
// A cross-cutting, app-level concern rather than something feature/onboarding alone owns, so it
// lives in core like OutcomeFeedbackRepository.
interface ConsentRepository {
    val consentState: Flow<ConsentState>
    suspend fun setConsentGiven(given: Boolean)
    suspend fun setAiTrainingOptIn(optedIn: Boolean)
}
