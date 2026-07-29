package com.bugreel.pro.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.repository.ConsentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class ConsentUiEvent {
    data object NavigateNext : ConsentUiEvent()
}

sealed class ConsentIntent {
    data object Accept : ConsentIntent()
}

// CLAUDE.md Section 13 Track 3 / Section 18 — must appear on first launch before any capture or
// report activity; this is the service consent only, never bundled with the AI training opt-in
// (AiTrainingOptInScreen is a separate screen with its own confirmation).
@HiltViewModel
class ConsentViewModel @Inject constructor(
    private val consentRepository: ConsentRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<ConsentUiEvent>()
    val events: SharedFlow<ConsentUiEvent> = _events.asSharedFlow()

    fun onIntent(intent: ConsentIntent) {
        when (intent) {
            ConsentIntent.Accept -> viewModelScope.launch {
                consentRepository.setConsentGiven(true)
                _events.emit(ConsentUiEvent.NavigateNext)
            }
        }
    }
}
