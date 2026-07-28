package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.core.domain.model.ReportContext
import javax.inject.Inject

class GenerateSummaryUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(context: ReportContext): AiSuggestionResult<String> =
        aiRepository.generateSummary(context)
}
