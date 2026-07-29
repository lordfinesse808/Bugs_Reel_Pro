package com.bugreel.pro.feature.analytics.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bugreel.pro.feature.analytics.domain.AcceptanceRateResult
import com.bugreel.pro.feature.analytics.domain.AcceptanceRateUseCase
import com.bugreel.pro.feature.analytics.domain.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed class AnalyticsDashboardUiState {
    data object Loading : AnalyticsDashboardUiState()
    data object Empty : AnalyticsDashboardUiState()
    data class Loaded(
        val totalReports: Int,
        val acceptanceRate: AcceptanceRateResult,
        val qualityTrend: List<Int>,
        val estimatedMinutesSaved: Int
    ) : AnalyticsDashboardUiState()
}

@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    analyticsRepository: AnalyticsRepository,
    acceptanceRateUseCase: AcceptanceRateUseCase
) : ViewModel() {

    val uiState: StateFlow<AnalyticsDashboardUiState> = combine(
        analyticsRepository.getAllReports(),
        analyticsRepository.getAllOutcomeFeedback()
    ) { reports, feedback ->
        if (reports.isEmpty()) {
            AnalyticsDashboardUiState.Empty
        } else {
            AnalyticsDashboardUiState.Loaded(
                totalReports = reports.size,
                acceptanceRate = acceptanceRateUseCase.fromFeedback(feedback),
                qualityTrend = reports.sortedBy { it.createdAt }.mapNotNull { it.qualityScore?.total },
                estimatedMinutesSaved = reports.size * ESTIMATED_MINUTES_SAVED_PER_REPORT
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AnalyticsDashboardUiState.Loading)

    private companion object {
        // A rough, product-facing estimate (manual bug write-up + formatting vs. BugReel's
        // guided flow), not a measured value -- there is nothing else it could be derived from.
        const val ESTIMATED_MINUTES_SAVED_PER_REPORT = 8
        const val STOP_TIMEOUT_MS = 5000L
    }
}
