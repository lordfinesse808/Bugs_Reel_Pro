package com.bugreel.pro.feature.quality.data

import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// CLAUDE.md Section 1 / Section 23 — THE FLYWHEEL. The interface has existed since Sprint 1;
// this is its first implementation, added now because Section 16 item 10 requires
// preAiQualityScore to start accumulating in Sprint 3 (Architecture Rule 1: BuildReportUseCase
// may only reach Room through this repository interface, never OutcomeFeedbackDao directly).
class OutcomeFeedbackRepositoryImpl @Inject constructor(
    private val outcomeFeedbackDao: OutcomeFeedbackDao
) : OutcomeFeedbackRepository {

    override suspend fun insert(feedback: OutcomeFeedback) {
        outcomeFeedbackDao.insert(toEntity(feedback))
    }

    override fun queryByReportId(reportId: String): Flow<OutcomeFeedback?> =
        outcomeFeedbackDao.getAll().map { rows -> rows.firstOrNull { it.reportId == reportId }?.let(::toDomain) }

    private fun toEntity(feedback: OutcomeFeedback) = OutcomeFeedbackEntity(
        reportId = feedback.reportId,
        platformOutcome = feedback.platformOutcome.name,
        rejectionReason = feedback.rejectionReason,
        reportedByTesterAt = feedback.reportedByTesterAt.toEpochMilli(),
        preAiQualityScore = feedback.preAiQualityScore,
        postAiQualityScore = feedback.postAiQualityScore,
        testerOverrodeSuggestion = feedback.testerOverrodeSuggestion,
        aiSuggestionsAccepted = Json.encodeToString(feedback.aiSuggestionsAccepted),
        aiSuggestionsRejected = Json.encodeToString(feedback.aiSuggestionsRejected)
    )

    private fun toDomain(entity: OutcomeFeedbackEntity) = OutcomeFeedback(
        reportId = entity.reportId,
        platformOutcome = PlatformOutcome.valueOf(entity.platformOutcome),
        rejectionReason = entity.rejectionReason,
        reportedByTesterAt = Instant.ofEpochMilli(entity.reportedByTesterAt),
        preAiQualityScore = entity.preAiQualityScore,
        postAiQualityScore = entity.postAiQualityScore,
        testerOverrodeSuggestion = entity.testerOverrodeSuggestion,
        aiSuggestionsAccepted = Json.decodeFromString(entity.aiSuggestionsAccepted),
        aiSuggestionsRejected = Json.decodeFromString(entity.aiSuggestionsRejected)
    )
}
