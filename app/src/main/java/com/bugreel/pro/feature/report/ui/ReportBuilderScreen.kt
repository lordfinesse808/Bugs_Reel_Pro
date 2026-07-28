package com.bugreel.pro.feature.report.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.feature.report.domain.ValidationError

private const val TOTAL_STEPS = 5

@Composable
fun ReportBuilderScreen(
    onSubmitted: (String) -> Unit,
    onNavigateToEvidencePicker: () -> Unit,
    onNavigateToAiAssist: (ReportContext) -> Unit,
    onBack: () -> Unit,
    viewModel: ReportBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ReportBuilderUiEvent.NavigateToReportList -> onSubmitted(event.reportId)
                is ReportBuilderUiEvent.ShowValidationErrors ->
                    snackbarHostState.showSnackbar("Fix ${event.errors.size} field(s) before submitting")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                ReportBuilderUiState.Loading, ReportBuilderUiState.Submitting ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is ReportBuilderUiState.BuildingReport -> BuildingReportContent(
                    state = state,
                    onIntent = viewModel::onIntent,
                    onNavigateToEvidencePicker = onNavigateToEvidencePicker,
                    onNavigateToAiAssist = onNavigateToAiAssist,
                    onBack = onBack
                )
                is ReportBuilderUiState.Submitted -> Unit // handled by the LaunchedEffect above
                is ReportBuilderUiState.Error -> Text(
                    text = state.message,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BuildingReportContent(
    state: ReportBuilderUiState.BuildingReport,
    onIntent: (ReportBuilderIntent) -> Unit,
    onNavigateToEvidencePicker: () -> Unit,
    onNavigateToAiAssist: (ReportContext) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        StepHeader(currentStep = state.currentStep)

        Column(modifier = Modifier.weight(1f).padding(top = 16.dp)) {
            when (state.currentStep) {
                1 -> StepTitleAndDescription(state, onIntent)
                2 -> StepSeverityAndFrequency(state, onIntent)
                3 -> StepReproSteps(state, onIntent)
                4 -> StepEvidence(state, onNavigateToEvidencePicker)
                5 -> StepReview(state, onNavigateToAiAssist)
            }
        }

        StepNavigation(
            currentStep = state.currentStep,
            canSubmit = state.errors.isEmpty(),
            onBack = { if (state.currentStep == 1) onBack() else onIntent(ReportBuilderIntent.StepChanged(state.currentStep - 1)) },
            onNext = { onIntent(ReportBuilderIntent.StepChanged(state.currentStep + 1)) },
            onSubmit = { onIntent(ReportBuilderIntent.Submit) }
        )
    }
}

@Composable
private fun StepHeader(currentStep: Int) {
    Text(
        text = "Step $currentStep of $TOTAL_STEPS",
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
private fun StepNavigation(
    currentStep: Int,
    canSubmit: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        OutlinedButton(onClick = onBack) { Text("Back") }
        if (currentStep < TOTAL_STEPS) {
            Button(onClick = onNext) { Text("Next") }
        } else {
            Button(onClick = onSubmit, enabled = canSubmit) { Text("Submit") }
        }
    }
}

@Composable
private fun StepTitleAndDescription(
    state: ReportBuilderUiState.BuildingReport,
    onIntent: (ReportBuilderIntent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FieldWithError(state.errors, field = "title") {
            OutlinedTextField(
                value = state.draft.title.orEmpty(),
                onValueChange = { onIntent(ReportBuilderIntent.TitleChanged(it)) },
                label = { Text("Title") },
                isError = state.errors.any { it.field == "title" },
                modifier = Modifier.fillMaxWidth()
            )
        }
        FieldWithError(state.errors, field = "summary") {
            OutlinedTextField(
                value = state.draft.summary.orEmpty(),
                onValueChange = { onIntent(ReportBuilderIntent.SummaryChanged(it)) },
                label = { Text("Description") },
                isError = state.errors.any { it.field == "summary" },
                modifier = Modifier.fillMaxWidth()
            )
        }
        PlatformDropdown(
            selected = state.draft.platform ?: TestPlatform.OTHER,
            onSelected = { onIntent(ReportBuilderIntent.PlatformChanged(it)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformDropdown(selected: TestPlatform, onSelected: (TestPlatform) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Platform") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth()
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TestPlatform.entries.forEach { platform ->
                DropdownMenuItem(
                    text = { Text(platform.displayName) },
                    onClick = {
                        onSelected(platform)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun StepSeverityAndFrequency(
    state: ReportBuilderUiState.BuildingReport,
    onIntent: (ReportBuilderIntent) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column {
            Text("Severity", fontWeight = FontWeight.Bold)
            Severity.entries.forEach { severity ->
                SelectableRow(
                    label = severity.displayName,
                    selected = state.draft.severity == severity,
                    onClick = { onIntent(ReportBuilderIntent.SeverityChanged(severity)) }
                )
            }
        }
        Column {
            Text("Frequency", fontWeight = FontWeight.Bold)
            Frequency.entries.forEach { frequency ->
                SelectableRow(
                    label = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    selected = state.draft.frequency == frequency,
                    onClick = { onIntent(ReportBuilderIntent.FrequencyChanged(frequency)) }
                )
            }
        }
    }
}

@Composable
private fun SelectableRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(if (selected) "✓ $label" else label)
    }
}

@Composable
private fun StepReproSteps(
    state: ReportBuilderUiState.BuildingReport,
    onIntent: (ReportBuilderIntent) -> Unit
) {
    var newStep by remember { mutableStateOf("") }
    val steps = state.draft.reproSteps.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(steps.size) { index ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}. ${steps[index]}", modifier = Modifier.weight(1f))
                    IconButton(
                        enabled = index > 0,
                        onClick = {
                            val reordered = steps.toMutableList()
                            reordered.add(index - 1, reordered.removeAt(index))
                            onIntent(ReportBuilderIntent.ReproStepsReordered(reordered))
                        }
                    ) { Text("↑") }
                    IconButton(
                        enabled = index < steps.size - 1,
                        onClick = {
                            val reordered = steps.toMutableList()
                            reordered.add(index + 1, reordered.removeAt(index))
                            onIntent(ReportBuilderIntent.ReproStepsReordered(reordered))
                        }
                    ) { Text("↓") }
                    IconButton(onClick = { onIntent(ReportBuilderIntent.ReproStepRemoved(index)) }) {
                        Text("✕")
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newStep,
                onValueChange = { newStep = it },
                label = { Text("Add step") },
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    if (newStep.isNotBlank()) {
                        onIntent(ReportBuilderIntent.ReproStepAdded(newStep))
                        newStep = ""
                    }
                }
            ) { Text("Add") }
        }
        if (state.errors.any { it.field == "reproSteps" }) {
            Text(
                "At least one repro step is required",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StepEvidence(state: ReportBuilderUiState.BuildingReport, onNavigateToEvidencePicker: () -> Unit) {
    val evidence = state.draft.evidenceBundle
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recording: ${if (evidence?.recordingUri != null) "attached" else "none"}")
        Text("Screenshots: ${evidence?.screenshots?.size ?: 0}")
        Text("Log text: ${if (evidence?.logText.isNullOrBlank()) "none" else "attached"}")
        Button(onClick = onNavigateToEvidencePicker) { Text("Add Evidence") }
        if (state.errors.any { it.field == "evidence" }) {
            Text(
                "Attach a recording or at least one screenshot",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun StepReview(
    state: ReportBuilderUiState.BuildingReport,
    onNavigateToAiAssist: (ReportContext) -> Unit
) {
    val draft = state.draft
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ReviewRow("Title", draft.title.orEmpty()) }
        item { ReviewRow("Description", draft.summary.orEmpty()) }
        item { ReviewRow("Platform", (draft.platform ?: TestPlatform.OTHER).displayName) }
        item { ReviewRow("Severity", draft.severity?.displayName.orEmpty()) }
        item { ReviewRow("Frequency", draft.frequency?.name.orEmpty()) }
        item { ReviewRow("Repro steps", "${draft.reproSteps.orEmpty().size} step(s)") }
        item { ReviewRow("Evidence", "${draft.evidenceBundle?.screenshots?.size ?: 0} screenshot(s)") }
        val deviceProfile = draft.deviceProfile
        val evidenceBundle = draft.evidenceBundle
        if (deviceProfile != null && evidenceBundle != null) {
            item {
                OutlinedButton(
                    onClick = {
                        onNavigateToAiAssist(
                            ReportContext(draft = draft, deviceProfile = deviceProfile, evidenceBundle = evidenceBundle)
                        )
                    }
                ) { Text("AI Assist") }
            }
        }
        if (state.errors.isNotEmpty()) {
            item {
                Text(
                    "${state.errors.size} field(s) still need attention",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(value, modifier = Modifier.weight(2f))
    }
}

@Composable
private fun FieldWithError(errors: List<ValidationError>, field: String, content: @Composable () -> Unit) {
    Column {
        content()
        errors.firstOrNull { it.field == field }?.let {
            Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
