package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext

// CLAUDE.md Section 9 — implementation (OpenAIService) lands in Sprint 4. Declared now,
// interface-only, so NetworkModule can bind a stub and the DI graph resolves end-to-end.
interface AIService {
    suspend fun generateTitle(context: ReportContext): AIResult<String>
    suspend fun generateSummary(context: ReportContext): AIResult<String>
    suspend fun formatReproSteps(context: ReportContext): AIResult<List<String>>
    suspend fun validateSafety(context: ReportContext, output: AiOutput): SafetyResult
}
