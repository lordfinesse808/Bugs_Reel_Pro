package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.core.domain.model.ReportContext

// What the AI Assist UI actually needs back for one field: either a usable suggestion, or the
// original tester input plus why it isn't being shown (blocked/rate-limited/failed) -- CLAUDE.md
// Section 9's "never show blank or freeze" / "return the original unmodified tester input"
// contract, generalized across title/summary/reproSteps.
sealed class AiSuggestionResult<out T> {
    data class Suggested<T>(val value: T) : AiSuggestionResult<T>()
    data class Blocked<T>(val originalValue: T?, val reason: String) : AiSuggestionResult<T>()
    data class RateLimited<T>(val retryAfterSeconds: Int) : AiSuggestionResult<T>()
    data class Failed<T>(val message: String, val originalValue: T?) : AiSuggestionResult<T>()
}

// Feature-scoped repository interface (mirrors feature/quality/domain/QualityRepository from
// Sprint 3) -- wraps AIService + AiRateLimiter + SafetyValidator behind one seam so
// GenerateTitleUseCase/GenerateSummaryUseCase/FormatReproStepsUseCase stay thin.
interface AiRepository {
    suspend fun generateTitle(context: ReportContext): AiSuggestionResult<String>
    suspend fun generateSummary(context: ReportContext): AiSuggestionResult<String>
    suspend fun formatReproSteps(context: ReportContext): AiSuggestionResult<List<String>>
}
