package com.bugreel.pro.feature.analytics.ui

import androidx.lifecycle.ViewModel
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.feature.analytics.domain.PracticeModeUseCase
import com.bugreel.pro.feature.analytics.domain.PracticeScenario
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PracticeModeUiState(
    val scenario: PracticeScenario,
    val attemptText: String = "",
    val score: QualityScore? = null
)

sealed class PracticeModeIntent {
    data class AttemptTextChanged(val value: String) : PracticeModeIntent()
    data object ScoreAttempt : PracticeModeIntent()
    data object TryAnotherScenario : PracticeModeIntent()
}

// CLAUDE.md Section 18 key implementation note — immediate score, "improve this" retry loop.
@HiltViewModel
class PracticeModeViewModel @Inject constructor(
    private val practiceModeUseCase: PracticeModeUseCase
) : ViewModel() {

    private val scenarios = practiceModeUseCase.scenarios()

    private val _uiState = MutableStateFlow(PracticeModeUiState(scenario = scenarios.first()))
    val uiState: StateFlow<PracticeModeUiState> = _uiState.asStateFlow()

    fun onIntent(intent: PracticeModeIntent) {
        when (intent) {
            is PracticeModeIntent.AttemptTextChanged ->
                _uiState.value = _uiState.value.copy(attemptText = intent.value, score = null)
            PracticeModeIntent.ScoreAttempt -> scoreAttempt()
            PracticeModeIntent.TryAnotherScenario -> nextScenario()
        }
    }

    private fun scoreAttempt() {
        val draft = practiceDraft(_uiState.value.attemptText)
        _uiState.value = _uiState.value.copy(score = practiceModeUseCase.score(draft))
    }

    private fun nextScenario() {
        val current = _uiState.value.scenario
        val next = scenarios.filterNot { it.id == current.id }.randomOrNull() ?: current
        _uiState.value = PracticeModeUiState(scenario = next)
    }

    private fun practiceDraft(summary: String) = BugReportDraft(
        id = "practice",
        title = null,
        summary = summary,
        reproSteps = summary.split("\n").map { it.trim() }.filter { it.isNotBlank() },
        severity = Severity.MEDIUM,
        frequency = Frequency.SOMETIMES,
        deviceProfile = null,
        evidenceBundle = null,
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.OTHER,
        createdAt = Instant.now(),
        updatedAt = null
    )
}
