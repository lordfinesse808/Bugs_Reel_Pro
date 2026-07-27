package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport

// Feature-scoped repository interface (mirrors feature/report/domain/PlatformTemplateRepository
// from Sprint 2) -- the one piece of IO ScoreReportUseCase needs: the tester's existing reports,
// for DuplicateDetector. Never called directly by a ViewModel (Architecture Rule 1).
interface QualityRepository {
    suspend fun getExistingReports(excludingId: String): List<BugReport>
}
