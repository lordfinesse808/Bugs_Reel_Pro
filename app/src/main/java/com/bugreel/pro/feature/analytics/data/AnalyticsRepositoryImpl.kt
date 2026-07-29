package com.bugreel.pro.feature.analytics.data

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import com.bugreel.pro.core.domain.repository.ReportRepository
import com.bugreel.pro.feature.analytics.domain.AnalyticsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// CLAUDE.md Section 18 — analytics reads Room only, through the existing core repositories;
// never Firestore/FirestoreSource (Architecture Rule 2).
class AnalyticsRepositoryImpl @Inject constructor(
    private val reportRepository: ReportRepository,
    private val outcomeFeedbackRepository: OutcomeFeedbackRepository
) : AnalyticsRepository {

    override fun getAllReports(): Flow<List<BugReport>> = reportRepository.getAllReports()

    override fun getAllOutcomeFeedback(): Flow<List<OutcomeFeedback>> = outcomeFeedbackRepository.getAll()
}
