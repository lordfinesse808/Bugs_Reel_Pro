package com.bugreel.pro.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// CLAUDE.md Section 17 — AI suggestions must be visually distinct from tester-written text (a
// tinted background here) so it's always clear which parts of the report came from the model.
@Composable
fun AiAssistScreen(
    onApply: (AiAssistResult) -> Unit,
    onCancel: () -> Unit,
    viewModel: AiAssistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is AiAssistUiEvent.Applied -> onApply(event.result)
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("AI Assist", style = MaterialTheme.typography.titleLarge)

            AiTextField(
                label = "Title",
                field = uiState.title,
                onGenerate = { viewModel.onIntent(AiAssistIntent.GenerateTitle) },
                onEdited = { viewModel.onIntent(AiAssistIntent.TitleEdited(it)) }
            )

            AiTextField(
                label = "Summary",
                field = uiState.summary,
                onGenerate = { viewModel.onIntent(AiAssistIntent.GenerateSummary) },
                onEdited = { viewModel.onIntent(AiAssistIntent.SummaryEdited(it)) }
            )

            AiReproStepsField(
                field = uiState.reproSteps,
                onGenerate = { viewModel.onIntent(AiAssistIntent.GenerateReproSteps) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { viewModel.onIntent(AiAssistIntent.Apply) }) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun AiTextField(
    label: String,
    field: AiFieldState<String?>,
    onGenerate: () -> Unit,
    onEdited: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onGenerate, enabled = !field.isLoading) {
                Text(if (field.suggestion == null) "Generate" else "Regenerate")
            }
        }
        if (field.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
        OutlinedTextField(
            value = field.current.orEmpty(),
            onValueChange = onEdited,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (field.suggestion != null) {
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                    } else {
                        Modifier
                    }
                )
        )
        AiFieldStatus(field)
    }
}

@Composable
private fun AiReproStepsField(field: AiFieldState<List<String>>, onGenerate: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Repro steps", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = onGenerate, enabled = !field.isLoading) {
                Text(if (field.suggestion == null) "Format with AI" else "Regenerate")
            }
        }
        if (field.isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }
        val backgroundModifier = if (field.suggestion != null) {
            Modifier.clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.tertiaryContainer)
        } else {
            Modifier
        }
        Column(modifier = Modifier.fillMaxWidth().then(backgroundModifier).padding(8.dp)) {
            field.current.forEachIndexed { index, step -> Text("${index + 1}. $step") }
        }
        AiFieldStatus(field)
    }
}

@Composable
private fun <T> AiFieldStatus(field: AiFieldState<T>) {
    field.blockedReason?.let {
        Text(
            "AI suggestion was blocked — showing your original text. ($it)",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    field.retryAfterSeconds?.let {
        Text(
            "You've reached the AI limit. Try again in $it seconds.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
    field.errorMessage?.let {
        Text(
            "Couldn't reach AI assist right now — your original text is unchanged.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
