package com.bugreel.pro.core.domain.repository

import com.bugreel.pro.core.domain.model.BugReport
import kotlinx.coroutines.flow.Flow

interface ReportRepository {
    fun getAllReports(): Flow<List<BugReport>>
    fun getReportById(id: String): Flow<BugReport?>
    suspend fun insertReport(report: BugReport)
    suspend fun updateReport(report: BugReport)
    suspend fun deleteReport(id: String)
}
