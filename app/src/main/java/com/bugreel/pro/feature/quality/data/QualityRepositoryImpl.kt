package com.bugreel.pro.feature.quality.data

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.repository.ReportRepository
import com.bugreel.pro.feature.quality.domain.QualityRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class QualityRepositoryImpl @Inject constructor(
    private val reportRepository: ReportRepository
) : QualityRepository {

    override suspend fun getExistingReports(excludingId: String): List<BugReport> =
        reportRepository.getAllReports().first().filter { it.id != excludingId }
}
