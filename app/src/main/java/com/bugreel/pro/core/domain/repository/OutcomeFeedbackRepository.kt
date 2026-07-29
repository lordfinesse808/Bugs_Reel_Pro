package com.bugreel.pro.core.domain.repository

import com.bugreel.pro.core.domain.model.OutcomeFeedback
import kotlinx.coroutines.flow.Flow

// CLAUDE.md Section 1 / Section 23 — THE FLYWHEEL.
interface OutcomeFeedbackRepository {
    suspend fun insert(feedback: OutcomeFeedback)
    fun queryByReportId(reportId: String): Flow<OutcomeFeedback?>
    // Sprint 5 — AcceptanceRateUseCase needs the full set to compute a rate across all reports.
    fun getAll(): Flow<List<OutcomeFeedback>>
}
