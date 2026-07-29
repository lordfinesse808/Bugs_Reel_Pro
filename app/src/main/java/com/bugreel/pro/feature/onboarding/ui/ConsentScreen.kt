package com.bugreel.pro.feature.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ConsentScreen(
    onAccepted: () -> Unit,
    viewModel: ConsentViewModel = hiltViewModel()
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ConsentUiEvent.NavigateNext -> onAccepted()
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Before you start testing", style = MaterialTheme.typography.titleLarge)
            Text(
                "BugReel Pro captures screen recordings, screenshots, and in-app interaction " +
                    "events while you're testing, and uses this evidence to build and score bug " +
                    "reports on your behalf. By continuing, you agree to this data being collected " +
                    "and stored to build your reports.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { viewModel.onIntent(ConsentIntent.Accept) }, modifier = Modifier.fillMaxWidth()) {
                Text("I agree, continue")
            }
        }
    }
}
