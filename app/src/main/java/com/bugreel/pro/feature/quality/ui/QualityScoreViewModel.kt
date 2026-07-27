package com.bugreel.pro.feature.quality.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.repository.ReportRepository
import com.bugreel.pro.feature.quality.domain.RecordInitialQualityScoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

@HiltViewModel
class QualityScoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reportRepository: ReportRepository,
    private val recordInitialQualityScoreUseCase: RecordInitialQualityScoreUseCase
) : ViewModel() {

    private val reportId: String = checkNotNull(savedStateHandle[REPORT_ID_ARG])

    private val _uiState = MutableStateFlow<QualityScoreUiState>(QualityScoreUiState.Loading)
    val uiState: StateFlow<QualityScoreUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<QualityScoreUiEvent>()
    val events: SharedFlow<QualityScoreUiEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val report = reportRepository.getReportById(reportId).first()
            if (report == null) {
                _uiState.value = QualityScoreUiState.NotFound
                return@launch
            }
            val scored = recordInitialQualityScoreUseCase(report)
            val score = scored.qualityScore ?: return@launch
            _uiState.value = QualityScoreUiState.Loaded(
                score = score,
                showUpgradePrompt = score.total < BuildConfig.QUALITY_SCORE_UPGRADE
            )
        }
    }

    fun onIntent(intent: QualityScoreIntent) {
        when (intent) {
            QualityScoreIntent.DoneClicked ->
                viewModelScope.launch { _events.emit(QualityScoreUiEvent.NavigateBack) }
        }
    }

    companion object {
        const val REPORT_ID_ARG = "reportId"
    }
}
