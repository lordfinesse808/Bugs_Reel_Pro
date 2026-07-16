package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext

// CLAUDE.md Section 9 / Anti-Pattern 2 — ALL prompt strings live in the Sprint 4
// implementation (DefaultPromptRepository) of this interface. Changing a prompt must touch
// exactly that one file, never a UseCase or inline call site.
interface PromptRepository {
    fun titlePrompt(context: ReportContext): String
    fun summaryPrompt(context: ReportContext): String
    fun reproStepsPrompt(context: ReportContext): String
    fun safetyValidationPrompt(context: ReportContext, output: AiOutput): String
}
