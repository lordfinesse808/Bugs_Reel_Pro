package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import javax.inject.Inject

// CLAUDE.md Section 9 / Anti-Pattern 2 — the ONLY file prompt strings live in. Changing a
// prompt means touching exactly this file, never a UseCase or OpenAIService itself. Every
// prompt is explicitly told to stick to the tester's own input -- SafetyValidator is the
// backstop, not the only defense against hallucination.
class DefaultPromptRepository @Inject constructor() : PromptRepository {

    override fun titlePrompt(context: ReportContext): String = buildString {
        appendLine("You are helping a professional QA tester write a concise bug report title.")
        appendLine("Tester's notes: ${context.draft.summary.orEmpty()}")
        appendLine("Severity: ${context.draft.severity?.displayName.orEmpty()}")
        appendLine("Repro steps so far: ${context.draft.reproSteps.orEmpty().joinToString("; ")}")
        appendLine(
            "Write exactly ONE title, under 100 characters, describing the bug and where it " +
                "happens. Base it only on the information above. No quotes, no trailing period."
        )
    }

    override fun summaryPrompt(context: ReportContext): String = buildString {
        appendLine("You are helping a professional QA tester write a clear bug summary for a crowdtesting platform.")
        appendLine("Title: ${context.draft.title.orEmpty()}")
        appendLine("Tester's notes: ${context.draft.summary.orEmpty()}")
        appendLine("Severity: ${context.draft.severity?.displayName.orEmpty()}")
        appendLine(
            "Device: ${context.deviceProfile.brand} ${context.deviceProfile.model}, " +
                "Android ${context.deviceProfile.osVersion}"
        )
        appendLine(
            "Write a 2-4 sentence summary of what went wrong, grounded only in the tester's " +
                "notes above. Never invent details, UI elements, or a device that isn't mentioned."
        )
    }

    override fun reproStepsPrompt(context: ReportContext): String = buildString {
        appendLine("You are formatting a professional QA tester's reproduction steps for a bug report.")
        appendLine("Tester's raw steps: ${context.draft.reproSteps.orEmpty().joinToString(" | ")}")
        appendLine("Interaction event log: ${context.evidenceBundle.eventLog.orEmpty()}")
        appendLine(
            "Rewrite these as a numbered list of clear, imperative steps, one per line. Do not " +
                "add, remove, or reorder steps beyond what the tester's input and event log support."
        )
    }

    override fun safetyValidationPrompt(context: ReportContext, output: AiOutput): String = buildString {
        appendLine("You are validating an AI-generated bug report against the evidence a tester actually captured.")
        appendLine("Generated title: ${output.generatedTitle.orEmpty()}")
        appendLine("Generated summary: ${output.generatedSummary.orEmpty()}")
        appendLine("Generated repro steps: ${output.formattedReproSteps.joinToString(" | ")}")
        appendLine("Severity claimed: ${context.draft.severity?.displayName.orEmpty()}")
        appendLine(
            "Evidence -- recording present: ${context.evidenceBundle.recordingUri != null}, " +
                "screenshots: ${context.evidenceBundle.screenshots.size}, " +
                "event log: ${context.evidenceBundle.eventLog.orEmpty()}"
        )
        appendLine(
            "Device: ${context.deviceProfile.brand} ${context.deviceProfile.model}, " +
                "Android ${context.deviceProfile.osVersion}"
        )
        appendLine(
            "Reply with exactly PASS if every claim above is fully supported by the evidence, " +
                "or BLOCKED: <reason> if any claim references something not in the evidence."
        )
    }
}
