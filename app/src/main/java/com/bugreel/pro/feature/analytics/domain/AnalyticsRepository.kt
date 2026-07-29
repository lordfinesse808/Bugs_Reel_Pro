package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import kotlinx.coroutines.flow.Flow

// Feature-scoped repository interface (mirrors feature/quality/domain/QualityRepository,
// feature/ai/domain/AiRepository) -- a thin facade over the two core repositories the
// analytics dashboard needs, so its UseCases/ViewModel depend on one seam instead of two.
interface AnalyticsRepository {
    fun getAllReports(): Flow<List<BugReport>>
    fun getAllOutcomeFeedback(): Flow<List<OutcomeFeedback>>
}
