package com.bugreel.pro.feature.report.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Shares the caller's ReportBuilderViewModel (scoped to the ReportBuilder nav back-stack
// entry via hiltViewModel(parentEntry) in AppNavGraph) rather than defaulting to its own
// instance — attached evidence must land back in the same in-progress draft.
@Composable
fun EvidencePickerScreen(
    onDone: () -> Unit,
    viewModel: ReportBuilderViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var logTextInput by remember { mutableStateOf("") }

    val recordingLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onIntent(ReportBuilderIntent.RecordingAttached(it)) } }

    val screenshotLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.onIntent(ReportBuilderIntent.ScreenshotAttached(it)) } }

    val evidence = (uiState as? ReportBuilderUiState.BuildingReport)?.draft?.evidenceBundle

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Attach Evidence", style = MaterialTheme.typography.headlineSmall)

        Text("Recording: ${if (evidence?.recordingUri != null) "attached" else "none"}")
        Button(onClick = { recordingLauncher.launch("video/mp4") }) { Text("Pick Recording (MP4)") }

        Text("Screenshots attached: ${evidence?.screenshots?.size ?: 0}")
        Button(onClick = { screenshotLauncher.launch("image/*") }) { Text("Add Screenshot") }

        OutlinedTextField(
            value = logTextInput,
            onValueChange = { logTextInput = it },
            label = { Text("Paste log text (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = {
                if (logTextInput.isNotBlank()) {
                    viewModel.onIntent(ReportBuilderIntent.LogTextAttached(logTextInput))
                }
            }
        ) { Text("Attach Log Text") }

        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
