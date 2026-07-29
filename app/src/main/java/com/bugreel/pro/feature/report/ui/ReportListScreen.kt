package com.bugreel.pro.feature.report.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ReportListViewModel @Inject constructor(
    reportRepository: ReportRepository
) : ViewModel() {
    val reports: StateFlow<List<BugReport>> = reportRepository.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}

// CLAUDE.md Section 15 — LazyColumn for 25+ report performance; empty state is required,
// never a blank screen or spinner.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportListScreen(
    onReportClick: (String) -> Unit,
    onStartCapture: () -> Unit,
    onPracticeMode: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: ReportListViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = { TextButton(onClick = onNavigateToAnalytics) { Text("Analytics") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reports.isEmpty()) {
                EmptyReportListState(onStartCapture = onStartCapture, onPracticeMode = onPracticeMode)
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    items(reports, key = { it.id }) { report ->
                        ReportRow(report = report, onClick = { onReportClick(report.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReportListState(onStartCapture: () -> Unit, onPracticeMode: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Your first report will appear here. Start by recording a test session.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStartCapture) { Text("Start Capture") }
        Spacer(modifier = Modifier.height(12.dp))
        // Practice Mode is Sprint 5 scope (CLAUDE.md Section 18) — stub link only, for now.
        TextButton(onClick = onPracticeMode) { Text("Try Practice Mode first ->") }
    }
}

@Composable
private fun ReportRow(report: BugReport, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(report.title.ifBlank { "Untitled report" }, style = MaterialTheme.typography.titleMedium)
            Text(report.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}
