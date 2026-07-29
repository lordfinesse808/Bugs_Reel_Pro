package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.OutcomeFeedback
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// CLAUDE.md Section 18 item 2 — computed from OutcomeFeedbackDao (via AnalyticsRepository, per
// Architecture Rule 1: UseCases call Repository interfaces, never a DAO directly).
data class AcceptanceRateResult(
    val accepted: Int,
    val rejected: Int,
    val pending: Int,
    // null (not 0.0) when there's no resolved outcome yet -- "no data" is not "0% acceptance".
    val rate: Double?
)

class AcceptanceRateUseCase @Inject constructor(
    private val analyticsRepository: AnalyticsRepository
) {
    suspend operator fun invoke(): AcceptanceRateResult = fromFeedback(analyticsRepository.getAllOutcomeFeedback().first())

    fun fromFeedback(feedback: List<OutcomeFeedback>): AcceptanceRateResult {
        val accepted = feedback.count { it.platformOutcome == PlatformOutcome.ACCEPTED }
        val rejected = feedback.count { it.platformOutcome == PlatformOutcome.REJECTED }
        val pending = feedback.count { it.platformOutcome == PlatformOutcome.PENDING }
        val resolved = accepted + rejected
        return AcceptanceRateResult(
            accepted = accepted,
            rejected = rejected,
            pending = pending,
            rate = if (resolved == 0) null else accepted.toDouble() / resolved
        )
    }
}
