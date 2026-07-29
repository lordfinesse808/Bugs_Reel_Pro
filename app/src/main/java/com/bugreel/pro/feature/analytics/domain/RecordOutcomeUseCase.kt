package com.bugreel.pro.feature.analytics.domain

import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.first

// CLAUDE.md Section 18 item 1 / Section 23 — THE FLYWHEEL. BuildReportUseCase.recordOutcomeFeedback
// (Sprint 3/4) already inserted this report's row at submission time with platformOutcome=PENDING;
// this use case is what a tester actually resolves it with once the platform responds.
class RecordOutcomeUseCase @Inject constructor(
    private val outcomeFeedbackRepository: OutcomeFeedbackRepository
) {
    suspend operator fun invoke(reportId: String, outcome: PlatformOutcome, rejectionReason: String?) {
        val existing = outcomeFeedbackRepository.queryByReportId(reportId).first()
            ?: error("No OutcomeFeedback row found for report $reportId -- was it ever submitted?")
        outcomeFeedbackRepository.insert(
            existing.copy(
                platformOutcome = outcome,
                rejectionReason = rejectionReason,
                reportedByTesterAt = Instant.now()
            )
        )
    }
}
