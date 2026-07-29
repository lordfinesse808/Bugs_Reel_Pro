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

sealed class AiTrainingOptInUiEvent {
    data object NavigateNext : AiTrainingOptInUiEvent()
}

sealed class AiTrainingOptInIntent {
    data class Continue(val optedIn: Boolean) : AiTrainingOptInIntent()
}

// CLAUDE.md Section 13 Track 3 / Section 18 — a distinct checkbox with its own confirmation,
// never bundled with ConsentScreen's service consent. Continuing without opting in is always a
// valid path (opting in is never required to use the app).
@HiltViewModel
class AiTrainingOptInViewModel @Inject constructor(
    private val consentRepository: ConsentRepository
) : ViewModel() {

    private val _events = MutableSharedFlow<AiTrainingOptInUiEvent>()
    val events: SharedFlow<AiTrainingOptInUiEvent> = _events.asSharedFlow()

    fun onIntent(intent: AiTrainingOptInIntent) {
        when (intent) {
            is AiTrainingOptInIntent.Continue -> viewModelScope.launch {
                consentRepository.setAiTrainingOptIn(intent.optedIn)
                _events.emit(AiTrainingOptInUiEvent.NavigateNext)
            }
        }
    }
}
