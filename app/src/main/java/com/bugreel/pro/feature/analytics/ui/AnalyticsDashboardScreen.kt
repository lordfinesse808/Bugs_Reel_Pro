package com.bugreel.pro.feature.analytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// CLAUDE.md Section 18 key implementation note — empty state shows the POTENTIAL of the chart
// (a dimmed placeholder of what acceptance rate / quality trend / time saved will look like),
// never just "No data yet".
@Composable
fun AnalyticsDashboardScreen(viewModel: AnalyticsDashboardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                AnalyticsDashboardUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                AnalyticsDashboardUiState.Empty -> EmptyDashboardState()
                is AnalyticsDashboardUiState.Loaded -> LoadedDashboard(state)
            }
        }
    }
}

@Composable
private fun EmptyDashboardState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).alpha(0.5f),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Submit your first report to start seeing your acceptance rate, quality trend, " +
                "and time saved here.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        MetricCard(title = "Acceptance rate", value = "--%")
        MetricCard(title = "Quality trend", value = "")
        MetricCard(title = "Time saved", value = "-- min")
    }
}

@Composable
private fun LoadedDashboard(state: AnalyticsDashboardUiState.Loaded) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rate = state.acceptanceRate.rate
        MetricCard(
            title = "Acceptance rate",
            value = if (rate != null) "${(rate * 100).toInt()}%" else "Pending review"
        )
        QualityTrendCard(state.qualityTrend)
        MetricCard(title = "Time saved", value = "${state.estimatedMinutesSaved} min")
        MetricCard(title = "Reports submitted", value = "${state.totalReports}")
    }
}

@Composable
private fun MetricCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun QualityTrendCard(trend: List<Int>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quality trend", style = MaterialTheme.typography.labelLarge)
            if (trend.isEmpty()) {
                Text("No scored reports yet", style = MaterialTheme.typography.bodyMedium)
            } else {
                TrendBars(trend)
            }
        }
    }
}

@Composable
private fun TrendBars(trend: List<Int>) {
    val maxHeight = 80.dp
    Row(
        modifier = Modifier.fillMaxWidth().height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        trend.takeLast(MAX_TREND_BARS).forEach { score ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(maxHeight * (score / MAX_SCORE))
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private const val MAX_TREND_BARS = 10
private const val MAX_SCORE = 100f
