package com.bugreel.pro.feature.report.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.core.domain.repository.ReportRepository
import com.bugreel.pro.core.util.DeviceProfiler
import com.bugreel.pro.feature.report.data.EvidenceManager
import com.bugreel.pro.feature.report.domain.BuildReportUseCase
import com.bugreel.pro.feature.report.domain.ValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportBuilderUiState {
    data object Loading : ReportBuilderUiState()
    data class BuildingReport(
        val draft: BugReportDraft,
        val errors: List<ValidationError>,
        val currentStep: Int = 1
    ) : ReportBuilderUiState()
    data object Submitting : ReportBuilderUiState()
    data class Submitted(val reportId: String) : ReportBuilderUiState()
    data class Error(val message: String) : ReportBuilderUiState()
}

sealed class ReportBuilderUiEvent {
    data class NavigateToReportList(val reportId: String) : ReportBuilderUiEvent()
    data class ShowValidationErrors(val errors: List<ValidationError>) : ReportBuilderUiEvent()
}

sealed class ReportBuilderIntent {
    data class TitleChanged(val value: String) : ReportBuilderIntent()
    data class SummaryChanged(val value: String) : ReportBuilderIntent()
    data class PlatformChanged(val value: TestPlatform) : ReportBuilderIntent()
    data class SeverityChanged(val value: Severity) : ReportBuilderIntent()
    data class FrequencyChanged(val value: Frequency) : ReportBuilderIntent()
    data class ReproStepAdded(val step: String) : ReportBuilderIntent()
    data class ReproStepRemoved(val index: Int) : ReportBuilderIntent()
    data class ReproStepsReordered(val steps: List<String>) : ReportBuilderIntent()
    data class RecordingAttached(val uri: Uri) : ReportBuilderIntent()
    data class ScreenshotAttached(val uri: Uri) : ReportBuilderIntent()
    data class LogTextAttached(val text: String) : ReportBuilderIntent()
    data class StepChanged(val step: Int) : ReportBuilderIntent()
    // Filled in by AppNavGraph after AiAssistScreen (feature/ai) returns its result -- kept to
    // primitives + the core AiOutput model (never feature/ai's own result type) so this file
    // never needs to import another feature package (Architecture Rule 4).
    data class AiSuggestionsApplied(
        val title: String?,
        val summary: String?,
        val reproSteps: List<String>,
        val aiOutput: AiOutput?
    ) : ReportBuilderIntent()
    data object Submit : ReportBuilderIntent()
}

@HiltViewModel
class ReportBuilderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildReportUseCase: BuildReportUseCase,
    private val reportRepository: ReportRepository,
    private val evidenceManager: EvidenceManager,
    private val deviceProfiler: DeviceProfiler
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportBuilderUiState>(ReportBuilderUiState.Loading)
    val uiState: StateFlow<ReportBuilderUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ReportBuilderUiEvent>()
    val events: SharedFlow<ReportBuilderUiEvent> = _events.asSharedFlow()

    init {
        _uiState.value = ReportBuilderUiState.BuildingReport(draft = newDraft(), errors = emptyList())
    }

    fun onIntent(intent: ReportBuilderIntent) {
        val current = _uiState.value as? ReportBuilderUiState.BuildingReport ?: return
        when (intent) {
            is ReportBuilderIntent.TitleChanged -> update(current) { it.copy(title = intent.value) }
            is ReportBuilderIntent.SummaryChanged -> update(current) { it.copy(summary = intent.value) }
            is ReportBuilderIntent.PlatformChanged -> update(current) { it.copy(platform = intent.value) }
            is ReportBuilderIntent.SeverityChanged -> update(current) { it.copy(severity = intent.value) }
            is ReportBuilderIntent.FrequencyChanged -> update(current) { it.copy(frequency = intent.value) }
            is ReportBuilderIntent.ReproStepAdded ->
                update(current) { it.copy(reproSteps = it.reproSteps.orEmpty() + intent.step) }
            is ReportBuilderIntent.ReproStepRemoved ->
                update(current) {
                    it.copy(reproSteps = it.reproSteps.orEmpty().filterIndexed { i, _ -> i != intent.index })
                }
            is ReportBuilderIntent.ReproStepsReordered ->
                update(current) { it.copy(reproSteps = intent.steps) }
            is ReportBuilderIntent.RecordingAttached -> attachRecording(current, intent.uri)
            is ReportBuilderIntent.ScreenshotAttached -> attachScreenshot(current, intent.uri)
            is ReportBuilderIntent.LogTextAttached -> attachLogText(current, intent.text)
            is ReportBuilderIntent.StepChanged -> _uiState.value = current.copy(currentStep = intent.step)
            is ReportBuilderIntent.AiSuggestionsApplied ->
                update(current) {
                    it.copy(
                        title = intent.title,
                        summary = intent.summary,
                        reproSteps = intent.reproSteps,
                        aiOutput = intent.aiOutput
                    )
                }
            ReportBuilderIntent.Submit -> submit(current)
        }
    }

    private fun update(
        current: ReportBuilderUiState.BuildingReport,
        transform: (BugReportDraft) -> BugReportDraft
    ) {
        val updated = transform(current.draft).copy(updatedAt = Instant.now())
        _uiState.value = current.copy(draft = updated, errors = buildReportUseCase(updated))
    }

    private fun attachRecording(current: ReportBuilderUiState.BuildingReport, uri: Uri) {
        evidenceManager.validateAndAttachRecording(uri).onSuccess {
            update(current) { it.copy(evidenceBundle = evidenceManager.getEvidenceBundle()) }
        }
    }

    private fun attachScreenshot(current: ReportBuilderUiState.BuildingReport, uri: Uri) {
        evidenceManager.validateAndAttachScreenshot(uri).onSuccess {
            update(current) { it.copy(evidenceBundle = evidenceManager.getEvidenceBundle()) }
        }
    }

    private fun attachLogText(current: ReportBuilderUiState.BuildingReport, text: String) {
        evidenceManager.attachLogText(text).onSuccess {
            update(current) { it.copy(evidenceBundle = evidenceManager.getEvidenceBundle()) }
        }
    }

    private fun submit(current: ReportBuilderUiState.BuildingReport) {
        val errors = buildReportUseCase(current.draft)
        if (errors.isNotEmpty()) {
            _uiState.value = current.copy(errors = errors)
            viewModelScope.launch { _events.emit(ReportBuilderUiEvent.ShowValidationErrors(errors)) }
            return
        }
        val report = buildReportUseCase.buildOrNull(current.draft)
        if (report == null) {
            _uiState.value = ReportBuilderUiState.Error("Could not build report from this draft")
            return
        }
        _uiState.value = ReportBuilderUiState.Submitting
        viewModelScope.launch {
            reportRepository.insertReport(report)
            buildReportUseCase.recordOutcomeFeedback(report)
            _uiState.value = ReportBuilderUiState.Submitted(report.id)
            _events.emit(ReportBuilderUiEvent.NavigateToReportList(report.id))
        }
    }

    private fun newDraft(): BugReportDraft {
        val now = Instant.now()
        return BugReportDraft(
            id = UUID.randomUUID().toString(),
            title = null,
            summary = null,
            reproSteps = null,
            severity = null,
            frequency = null,
            deviceProfile = deviceProfiler.getDeviceProfile(context),
            evidenceBundle = evidenceManager.getEvidenceBundle(),
            qualityScore = null,
            aiOutput = null,
            platform = TestPlatform.OTHER,
            createdAt = now,
            updatedAt = now
        )
    }

}
