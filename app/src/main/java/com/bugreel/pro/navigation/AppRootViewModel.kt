package com.bugreel.pro.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.model.ConsentState
import com.bugreel.pro.core.domain.repository.ConsentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// CLAUDE.md Section 18 — "Consent screen must appear on first launch before any capture or
// report activity." Read once at app root, before AppNavGraph (and therefore every feature) is
// ever composed.
@HiltViewModel
class AppRootViewModel @Inject constructor(
    consentRepository: ConsentRepository
) : ViewModel() {

    val consentState: StateFlow<ConsentState?> = consentRepository.consentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
