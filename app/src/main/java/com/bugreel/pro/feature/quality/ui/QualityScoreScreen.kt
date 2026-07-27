package com.bugreel.pro.feature.quality.ui

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.domain.model.CoachingHint
import com.bugreel.pro.core.domain.model.QualityScore

// CLAUDE.md Section 16 — animated score reveal is a UX requirement, not cosmetic (mirrors the
// same note for Sprint 3's QualityScoreScreen); upgrade prompt shows only when total is below
// BuildConfig.QUALITY_SCORE_UPGRADE.
@Composable
fun QualityScoreScreen(
    onDone: () -> Unit,
    viewModel: QualityScoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                QualityScoreUiEvent.NavigateBack -> onDone()
            }
        }
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                QualityScoreUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                QualityScoreUiState.NotFound ->
                    Text(
                        "Report not found",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                is QualityScoreUiState.Loaded -> QualityScoreContent(
                    score = state.score,
                    showUpgradePrompt = state.showUpgradePrompt,
                    onDone = { viewModel.onIntent(QualityScoreIntent.DoneClicked) }
                )
            }
        }
    }
}

@Composable
private fun QualityScoreContent(score: QualityScore, showUpgradePrompt: Boolean, onDone: () -> Unit) {
    val animatedTotal by animateIntAsState(
        targetValue = score.total,
        animationSpec = tween(durationMillis = SCORE_REVEAL_DURATION_MS),
        label = "qualityScoreReveal"
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quality Score", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "$animatedTotal",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        if (showUpgradePrompt) {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Scores below ${BuildConfig.QUALITY_SCORE_UPGRADE} are less likely to be " +
                            "accepted. Follow the tips below to raise this report's score.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        DimensionRow("Reproduction", score.reproduction, MAX_REPRODUCTION)
        DimensionRow("Evidence", score.evidence, MAX_EVIDENCE)
        DimensionRow("Description", score.description, MAX_DESCRIPTION)
        DimensionRow("Severity", score.severity, MAX_SEVERITY)
        DimensionRow("Metadata", score.metadata, MAX_METADATA)

        if (score.coaching.isNotEmpty()) {
            Text(
                "Coaching tips",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(score.coaching, key = { it.field }) { hint -> CoachingHintRow(hint) }
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

@Composable
private fun DimensionRow(label: String, value: Int, max: Int) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: $value/$max", style = MaterialTheme.typography.bodyMedium)
        LinearProgressIndicator(
            progress = { value.toFloat() / max },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CoachingHintRow(hint: CoachingHint) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(hint.message, style = MaterialTheme.typography.bodyMedium)
            Text(
                "+${hint.scoreImpact} points",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private const val SCORE_REVEAL_DURATION_MS = 1000
private const val MAX_REPRODUCTION = 25
private const val MAX_EVIDENCE = 25
private const val MAX_DESCRIPTION = 20
private const val MAX_SEVERITY = 15
private const val MAX_METADATA = 15
