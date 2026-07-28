package com.bugreel.pro.feature.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.quality.QualityScorer
import com.bugreel.pro.feature.ai.domain.AiSuggestionResult
import com.bugreel.pro.feature.ai.domain.FormatReproStepsUseCase
import com.bugreel.pro.feature.ai.domain.GenerateSummaryUseCase
import com.bugreel.pro.feature.ai.domain.GenerateTitleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// CLAUDE.md Section 17 — result handed back to whoever launched this screen (ReportBuilder) once
// the tester applies. `title`/`summary`/`reproSteps` are the FINAL field values (AI-suggested or
// tester-edited); `aiOutput` is null if the tester never generated anything (matches the
// existing "no AI assist used" contract BuildReportUseCase.recordOutcomeFeedback() already
// handles), otherwise carries the flywheel bookkeeping (Section 23).
data class AiAssistResult(
    val title: String?,
    val summary: String?,
    val reproSteps: List<String>,
    val aiOutput: AiOutput?
)

data class AiFieldState<T>(
    val original: T,
    val current: T,
    val suggestion: T? = null,
    val isLoading: Boolean = false,
    val blockedReason: String? = null,
    val retryAfterSeconds: Int? = null,
    val errorMessage: String? = null
) {
    val wasOverridden: Boolean get() = suggestion != null && current != suggestion
}

data class AiAssistUiState(
    val title: AiFieldState<String?>,
    val summary: AiFieldState<String?>,
    val reproSteps: AiFieldState<List<String>>
)

sealed class AiAssistUiEvent {
    data class Applied(val result: AiAssistResult) : AiAssistUiEvent()
}

sealed class AiAssistIntent {
    data object GenerateTitle : AiAssistIntent()
    data object GenerateSummary : AiAssistIntent()
    data object GenerateReproSteps : AiAssistIntent()
    data class TitleEdited(val value: String) : AiAssistIntent()
    data class SummaryEdited(val value: String) : AiAssistIntent()
    data class ReproStepsEdited(val value: List<String>) : AiAssistIntent()
    data object Apply : AiAssistIntent()
}

@HiltViewModel
class AiAssistViewModel @Inject constructor(
    private val generateTitleUseCase: GenerateTitleUseCase,
    private val generateSummaryUseCase: GenerateSummaryUseCase,
    private val formatReproStepsUseCase: FormatReproStepsUseCase,
    private val qualityScorer: QualityScorer
) : ViewModel() {

    private lateinit var reportContext: ReportContext
    private var preAiQualityScore: Int = 0

    private val _uiState = MutableStateFlow(
        AiAssistUiState(
            title = AiFieldState(original = null, current = null),
            summary = AiFieldState(original = null, current = null),
            reproSteps = AiFieldState(original = emptyList(), current = emptyList())
        )
    )
    val uiState: StateFlow<AiAssistUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AiAssistUiEvent>()
    val events: SharedFlow<AiAssistUiEvent> = _events.asSharedFlow()

    // Called once by AiAssistScreen right after the ViewModel is created (Compose Navigation
    // hands the in-progress draft's context forward via the previous entry's SavedStateHandle --
    // see AppNavGraph). Re-running it on the same context is a no-op-ish reset, safe on
    // recomposition since draft/deviceProfile/evidenceBundle are stable within one visit.
    fun initialize(context: ReportContext) {
        reportContext = context
        preAiQualityScore = qualityScorer.score(context.draft).total
        _uiState.value = AiAssistUiState(
            title = AiFieldState(original = context.draft.title, current = context.draft.title),
            summary = AiFieldState(original = context.draft.summary, current = context.draft.summary),
            reproSteps = AiFieldState(
                original = context.draft.reproSteps.orEmpty(),
                current = context.draft.reproSteps.orEmpty()
            )
        )
    }

    fun onIntent(intent: AiAssistIntent) {
        when (intent) {
            AiAssistIntent.GenerateTitle -> generateTitle()
            AiAssistIntent.GenerateSummary -> generateSummary()
            AiAssistIntent.GenerateReproSteps -> generateReproSteps()
            is AiAssistIntent.TitleEdited ->
                _uiState.update { it.copy(title = it.title.copy(current = intent.value)) }
            is AiAssistIntent.SummaryEdited ->
                _uiState.update { it.copy(summary = it.summary.copy(current = intent.value)) }
            is AiAssistIntent.ReproStepsEdited ->
                _uiState.update { it.copy(reproSteps = it.reproSteps.copy(current = intent.value)) }
            AiAssistIntent.Apply -> apply()
        }
    }

    private fun generateTitle() {
        _uiState.update { it.copy(title = it.title.copy(isLoading = true, errorMessage = null)) }
        viewModelScope.launch {
            when (val result = generateTitleUseCase(reportContext)) {
                is AiSuggestionResult.Suggested -> _uiState.update {
                    it.copy(title = it.title.copy(isLoading = false, suggestion = result.value, current = result.value))
                }
                is AiSuggestionResult.Blocked -> _uiState.update {
                    it.copy(title = it.title.copy(isLoading = false, blockedReason = result.reason, current = result.originalValue))
                }
                is AiSuggestionResult.RateLimited -> _uiState.update {
                    it.copy(title = it.title.copy(isLoading = false, retryAfterSeconds = result.retryAfterSeconds))
                }
                is AiSuggestionResult.Failed -> _uiState.update {
                    it.copy(title = it.title.copy(isLoading = false, errorMessage = result.message, current = result.originalValue))
                }
            }
        }
    }

    private fun generateSummary() {
        _uiState.update { it.copy(summary = it.summary.copy(isLoading = true, errorMessage = null)) }
        viewModelScope.launch {
            when (val result = generateSummaryUseCase(reportContext)) {
                is AiSuggestionResult.Suggested -> _uiState.update {
                    it.copy(summary = it.summary.copy(isLoading = false, suggestion = result.value, current = result.value))
                }
                is AiSuggestionResult.Blocked -> _uiState.update {
                    it.copy(summary = it.summary.copy(isLoading = false, blockedReason = result.reason, current = result.originalValue))
                }
                is AiSuggestionResult.RateLimited -> _uiState.update {
                    it.copy(summary = it.summary.copy(isLoading = false, retryAfterSeconds = result.retryAfterSeconds))
                }
                is AiSuggestionResult.Failed -> _uiState.update {
                    it.copy(summary = it.summary.copy(isLoading = false, errorMessage = result.message, current = result.originalValue))
                }
            }
        }
    }

    private fun generateReproSteps() {
        _uiState.update { it.copy(reproSteps = it.reproSteps.copy(isLoading = true, errorMessage = null)) }
        viewModelScope.launch {
            when (val result = formatReproStepsUseCase(reportContext)) {
                is AiSuggestionResult.Suggested -> _uiState.update {
                    it.copy(reproSteps = it.reproSteps.copy(isLoading = false, suggestion = result.value, current = result.value))
                }
                is AiSuggestionResult.Blocked -> _uiState.update {
                    it.copy(
                        reproSteps = it.reproSteps.copy(
                            isLoading = false,
                            blockedReason = result.reason,
                            current = result.originalValue ?: it.reproSteps.original
                        )
                    )
                }
                is AiSuggestionResult.RateLimited -> _uiState.update {
                    it.copy(reproSteps = it.reproSteps.copy(isLoading = false, retryAfterSeconds = result.retryAfterSeconds))
                }
                is AiSuggestionResult.Failed -> _uiState.update {
                    it.copy(
                        reproSteps = it.reproSteps.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            current = result.originalValue ?: it.reproSteps.original
                        )
                    )
                }
            }
        }
    }

    private fun apply() {
        val state = _uiState.value
        val accepted = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        if (state.title.suggestion != null) {
            (if (state.title.wasOverridden) rejected else accepted) += "title"
        }
        if (state.summary.suggestion != null) {
            (if (state.summary.wasOverridden) rejected else accepted) += "summary"
        }
        if (state.reproSteps.suggestion != null) {
            (if (state.reproSteps.wasOverridden) rejected else accepted) += "reproSteps"
        }

        val finalDraft = reportContext.draft.copy(
            title = state.title.current,
            summary = state.summary.current,
            reproSteps = state.reproSteps.current
        )
        val anyAiUsed = state.title.suggestion != null || state.summary.suggestion != null ||
            state.reproSteps.suggestion != null
        val postAiQualityScore = if (anyAiUsed) qualityScorer.score(finalDraft).total else 0

        val aiOutput = AiOutput(
            generatedTitle = state.title.suggestion,
            generatedSummary = state.summary.suggestion,
            formattedReproSteps = state.reproSteps.suggestion.orEmpty(),
            safetyPassed = listOf(state.title, state.summary, state.reproSteps).none { it.blockedReason != null },
            blockedReason = listOfNotNull(
                state.title.blockedReason,
                state.summary.blockedReason,
                state.reproSteps.blockedReason
            ).firstOrNull(),
            preAiQualityScore = preAiQualityScore,
            postAiQualityScore = postAiQualityScore,
            testerOverrodeSuggestion = rejected.isNotEmpty(),
            aiSuggestionsAccepted = accepted,
            aiSuggestionsRejected = rejected
        )

        viewModelScope.launch {
            _events.emit(
                AiAssistUiEvent.Applied(
                    AiAssistResult(
                        title = state.title.current,
                        summary = state.summary.current,
                        reproSteps = state.reproSteps.current,
                        aiOutput = if (anyAiUsed) aiOutput else null
                    )
                )
            )
        }
    }
}
