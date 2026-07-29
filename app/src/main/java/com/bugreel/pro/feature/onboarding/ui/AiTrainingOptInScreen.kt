package com.bugreel.pro.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AiTrainingOptInScreen(
    onContinue: () -> Unit,
    viewModel: AiTrainingOptInViewModel = hiltViewModel()
) {
    var optedIn by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AiTrainingOptInUiEvent.NavigateNext -> onContinue()
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Help improve BugReel's AI", style = MaterialTheme.typography.titleLarge)
            Text(
                "You can optionally let BugReel use your accepted and rejected reports to " +
                    "improve its AI suggestions for everyone. This is completely separate from " +
                    "the service consent you already gave, and is never required.",
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = optedIn, onCheckedChange = { optedIn = it })
                Text("Yes, use my reports to help train BugReel's AI")
            }
            Button(
                onClick = { viewModel.onIntent(AiTrainingOptInIntent.Continue(optedIn)) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Continue") }
        }
    }
}
