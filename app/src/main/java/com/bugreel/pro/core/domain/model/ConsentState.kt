package com.bugreel.pro.core.domain.model

// Domain projection of core/data/local/onboarding/ConsentPreferences (the DataStore-persisted
// shape) -- kept separate per Architecture Rule 1 (domain must not depend on a data-layer type),
// the same way OutcomeFeedback is the domain model for OutcomeFeedbackEntity.
data class ConsentState(
    val consentGiven: Boolean,
    val aiTrainingOptIn: Boolean
)
