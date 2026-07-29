package com.bugreel.pro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugreel.pro.feature.onboarding.ui.AiTrainingOptInScreen
import com.bugreel.pro.feature.onboarding.ui.ConsentScreen

private enum class OnboardingStep { CONSENT, AI_OPT_IN, DONE }

// CLAUDE.md Section 18 — gates the entire app behind consent, resolved once. `step` is only
// ever initialized from the persisted flag on the first composition where it's known; once the
// tester starts the onboarding flow, later recompositions (e.g. the DataStore write from
// accepting consent) must not un-set an in-progress AI_OPT_IN step back to DONE.
@Composable
fun AppRoot(viewModel: AppRootViewModel = hiltViewModel()) {
    val consentState by viewModel.consentState.collectAsStateWithLifecycle()
    val resolvedState = consentState ?: run {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }

    var step by remember { mutableStateOf<OnboardingStep?>(null) }
    if (step == null) {
        step = if (resolvedState.consentGiven) OnboardingStep.DONE else OnboardingStep.CONSENT
    }

    when (step) {
        OnboardingStep.CONSENT -> ConsentScreen(onAccepted = { step = OnboardingStep.AI_OPT_IN })
        OnboardingStep.AI_OPT_IN -> AiTrainingOptInScreen(onContinue = { step = OnboardingStep.DONE })
        OnboardingStep.DONE, null -> AppNavGraph()
    }
}
