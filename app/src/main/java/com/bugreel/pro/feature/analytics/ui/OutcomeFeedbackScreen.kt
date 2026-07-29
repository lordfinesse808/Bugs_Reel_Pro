package com.bugreel.pro.feature.analytics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome

// CLAUDE.md Section 18 key implementation note — must be reachable from a completed, submitted
// report, never buried in settings (wired from QualityScoreScreen; see AppNavGraph).
@Composable
fun OutcomeFeedbackScreen(
    onDone: () -> Unit,
    viewModel: OutcomeFeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OutcomeFeedbackUiEvent.NavigateBack -> onDone()
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("What did the platform decide?", style = MaterialTheme.typography.titleLarge)

            OutcomeOption(
                label = "Accepted",
                selected = uiState.outcome == PlatformOutcome.ACCEPTED,
                onClick = { viewModel.onIntent(OutcomeFeedbackIntent.OutcomeSelected(PlatformOutcome.ACCEPTED)) }
            )
            OutcomeOption(
                label = "Rejected",
                selected = uiState.outcome == PlatformOutcome.REJECTED,
                onClick = { viewModel.onIntent(OutcomeFeedbackIntent.OutcomeSelected(PlatformOutcome.REJECTED)) }
            )

            if (uiState.outcome == PlatformOutcome.REJECTED) {
                OutlinedTextField(
                    value = uiState.rejectionReason,
                    onValueChange = { viewModel.onIntent(OutcomeFeedbackIntent.RejectionReasonChanged(it)) },
                    label = { Text("Why was it rejected?") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { viewModel.onIntent(OutcomeFeedbackIntent.Submit) },
                enabled = uiState.outcome != null && !uiState.submitting
            ) { Text("Submit") }
        }
    }
}

@Composable
private fun OutcomeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(top = 12.dp))
    }
}
