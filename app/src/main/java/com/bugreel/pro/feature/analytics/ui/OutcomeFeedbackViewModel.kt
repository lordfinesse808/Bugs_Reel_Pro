package com.bugreel.pro.feature.analytics.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.feature.analytics.domain.RecordOutcomeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// CLAUDE.md Section 18 item 5 / Section 23 — where the tester reports what the platform
// actually decided; this is the flywheel's second half (BuildReportUseCase.recordOutcomeFeedback
// wrote the PENDING row with pre/postAiQualityScore at submission time).
data class OutcomeFeedbackUiState(
    val outcome: PlatformOutcome? = null,
    val rejectionReason: String = "",
    val submitting: Boolean = false
)

sealed class OutcomeFeedbackUiEvent {
    data object NavigateBack : OutcomeFeedbackUiEvent()
}

sealed class OutcomeFeedbackIntent {
    data class OutcomeSelected(val outcome: PlatformOutcome) : OutcomeFeedbackIntent()
    data class RejectionReasonChanged(val value: String) : OutcomeFeedbackIntent()
    data object Submit : OutcomeFeedbackIntent()
}

@HiltViewModel
class OutcomeFeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordOutcomeUseCase: RecordOutcomeUseCase
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle[REPORT_ID_ARG])

    private val _uiState = MutableStateFlow(OutcomeFeedbackUiState())
    val uiState: StateFlow<OutcomeFeedbackUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OutcomeFeedbackUiEvent>()
    val events: SharedFlow<OutcomeFeedbackUiEvent> = _events.asSharedFlow()

    fun onIntent(intent: OutcomeFeedbackIntent) {
        when (intent) {
            is OutcomeFeedbackIntent.OutcomeSelected ->
                _uiState.value = _uiState.value.copy(outcome = intent.outcome)
            is OutcomeFeedbackIntent.RejectionReasonChanged ->
                _uiState.value = _uiState.value.copy(rejectionReason = intent.value)
            OutcomeFeedbackIntent.Submit -> submit()
        }
    }

    private fun submit() {
        val outcome = _uiState.value.outcome ?: return
        _uiState.value = _uiState.value.copy(submitting = true)
        viewModelScope.launch {
            val reason = _uiState.value.rejectionReason.takeIf { outcome == PlatformOutcome.REJECTED && it.isNotBlank() }
            recordOutcomeUseCase(reportId, outcome, reason)
            _events.emit(OutcomeFeedbackUiEvent.NavigateBack)
        }
    }

    companion object {
        const val REPORT_ID_ARG = "reportId"
    }
}
