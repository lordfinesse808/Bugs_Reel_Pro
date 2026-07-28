package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.core.domain.model.ReportContext
import javax.inject.Inject

class FormatReproStepsUseCase @Inject constructor(
    private val aiRepository: AiRepository
) {
    suspend operator fun invoke(context: ReportContext): AiSuggestionResult<List<String>> =
        aiRepository.formatReproSteps(context)
}
