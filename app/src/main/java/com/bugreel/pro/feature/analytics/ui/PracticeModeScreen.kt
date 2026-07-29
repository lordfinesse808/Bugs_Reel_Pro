package com.bugreel.pro.feature.analytics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PracticeModeScreen(
    onBack: () -> Unit,
    viewModel: PracticeModeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Practice Mode", style = MaterialTheme.typography.titleLarge)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(uiState.scenario.prompt, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        uiState.scenario.hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = uiState.attemptText,
                onValueChange = { viewModel.onIntent(PracticeModeIntent.AttemptTextChanged(it)) },
                label = { Text("Write your bug report attempt") },
                modifier = Modifier.fillMaxWidth()
            )

            uiState.score?.let { score ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Score: ${score.total}/100", style = MaterialTheme.typography.headlineMedium)
                        score.coaching.forEach { hint ->
                            Text("• ${hint.message}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Button(onClick = { viewModel.onIntent(PracticeModeIntent.ScoreAttempt) }) { Text("Score it") }
            OutlinedButton(onClick = { viewModel.onIntent(PracticeModeIntent.TryAnotherScenario) }) {
                Text("Try another scenario")
            }
            OutlinedButton(onClick = onBack) { Text("Done practicing") }
        }
    }
}
