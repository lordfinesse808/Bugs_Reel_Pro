package com.bugreel.pro.feature.ai.data

import com.bugreel.pro.core.data.remote.ai.AIResult
import com.bugreel.pro.core.data.remote.ai.AIService
import com.bugreel.pro.core.data.remote.ai.SafetyResult
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.feature.ai.domain.AiRateLimiter
import com.bugreel.pro.feature.ai.domain.AiRepository
import com.bugreel.pro.feature.ai.domain.AiSuggestionResult
import javax.inject.Inject

// CLAUDE.md Section 9 — every AI suggestion goes: rate limit check -> AIService call ->
// SafetyValidator (via AIService.validateSafety) before ever reaching the UI. On block, returns
// the tester's original input alongside the reason (Anti-Pattern 7 — no bypass path).
class AiRepositoryImpl @Inject constructor(
    private val aiService: AIService,
    private val aiRateLimiter: AiRateLimiter
) : AiRepository {

    override suspend fun generateTitle(context: ReportContext): AiSuggestionResult<String> =
        generate(
            context = context,
            original = context.draft.title,
            call = { aiService.generateTitle(context) },
            toCandidateOutput = { value -> emptyOutput().copy(generatedTitle = value) }
        )

    override suspend fun generateSummary(context: ReportContext): AiSuggestionResult<String> =
        generate(
            context = context,
            original = context.draft.summary,
            call = { aiService.generateSummary(context) },
            toCandidateOutput = { value -> emptyOutput().copy(generatedSummary = value) }
        )

    override suspend fun formatReproSteps(context: ReportContext): AiSuggestionResult<List<String>> =
        generate(
            context = context,
            original = context.draft.reproSteps.orEmpty(),
            call = { aiService.formatReproSteps(context) },
            toCandidateOutput = { value -> emptyOutput().copy(formattedReproSteps = value) }
        )

    private suspend fun <T> generate(
        context: ReportContext,
        original: T?,
        call: suspend () -> AIResult<T>,
        toCandidateOutput: (T) -> AiOutput
    ): AiSuggestionResult<T> {
        val limitCheck = aiRateLimiter.tryAcquire(System.currentTimeMillis())
        if (limitCheck is AIResult.RateLimitExceeded) {
            return AiSuggestionResult.RateLimited(limitCheck.retryAfterSeconds)
        }
        return when (val result = call()) {
            is AIResult.Success -> {
                when (val safety = aiService.validateSafety(context, toCandidateOutput(result.value))) {
                    SafetyResult.Pass -> AiSuggestionResult.Suggested(result.value)
                    is SafetyResult.Blocked -> AiSuggestionResult.Blocked(original, safety.reason)
                }
            }
            is AIResult.RateLimitExceeded -> AiSuggestionResult.RateLimited(result.retryAfterSeconds)
            is AIResult.Error -> AiSuggestionResult.Failed(result.message, original)
        }
    }

    private fun emptyOutput() = AiOutput(
        generatedTitle = null,
        generatedSummary = null,
        formattedReproSteps = emptyList(),
        safetyPassed = false,
        blockedReason = null
    )
}
