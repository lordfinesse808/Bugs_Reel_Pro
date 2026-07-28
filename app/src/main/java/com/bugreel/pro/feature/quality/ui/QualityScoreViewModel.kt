package com.bugreel.pro.feature.quality.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class QualityScoreUiState {
    data object Loading : QualityScoreUiState()
    data class Loaded(val score: QualityScore, val showUpgradePrompt: Boolean) : QualityScoreUiState()
    data object NotFound : QualityScoreUiState()
}

sealed class QualityScoreUiEvent {
    data object NavigateBack : QualityScoreUiEvent()
}

sealed class QualityScoreIntent {
    data object DoneClicked : QualityScoreIntent()
}

// Sprint 4: no longer scores anything itself. BuildReportUseCase.buildOrNull() (feature/report)
// attaches qualityScore before the report is ever persisted (Section 16 amendment, revised
// Sprint 4 now that QualityScorer lives in core/domain/quality), so this screen just displays
// whatever score the report already carries.
@HiltViewModel
class QualityScoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    reportRepository: ReportRepository
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle[REPORT_ID_ARG])

    val uiState: StateFlow<QualityScoreUiState> = reportRepository.getReportById(reportId)
        .map { report -> toUiState(report) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), QualityScoreUiState.Loading)

    private val _events = MutableSharedFlow<QualityScoreUiEvent>()
    val events: SharedFlow<QualityScoreUiEvent> = _events.asSharedFlow()

    fun onIntent(intent: QualityScoreIntent) {
        when (intent) {
            QualityScoreIntent.DoneClicked ->
                viewModelScope.launch { _events.emit(QualityScoreUiEvent.NavigateBack) }
        }
    }

    private fun toUiState(report: BugReport?): QualityScoreUiState {
        if (report == null) return QualityScoreUiState.NotFound
        val score = report.qualityScore ?: return QualityScoreUiState.Loading
        return QualityScoreUiState.Loaded(
            score = score,
            showUpgradePrompt = score.total < BuildConfig.QUALITY_SCORE_UPGRADE
        )
    }

    companion object {
        const val REPORT_ID_ARG = "reportId"
        private const val STOP_TIMEOUT_MS = 5000L
    }
}
