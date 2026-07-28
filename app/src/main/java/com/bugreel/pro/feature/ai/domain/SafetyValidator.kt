package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.core.data.remote.ai.SafetyResult
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.model.enums.Severity
import javax.inject.Inject

// CLAUDE.md Section 9 / ADR-004 — the four hard, deterministic failure criteria. This is plain
// Kotlin string/keyword matching against the evidence bundle, not another AI call: OpenAIService
// delegates to this before ever showing AI output to the tester (Anti-Pattern 7 — no bypass
// path). Checks run in order; the first failure wins.
class SafetyValidator @Inject constructor() {

    fun validate(context: ReportContext, output: AiOutput): SafetyResult {
        val reason = checkUiElementsGrounded(context, output)
            ?: checkSeverityConsistency(context)
            ?: checkReproStepsGrounded(context, output)
            ?: checkDeviceMetadataConsistency(context, output)
        return reason?.let { SafetyResult.Blocked(it) } ?: SafetyResult.Pass
    }

    // Criterion (a) — references a UI element not present in the captured evidence. AI output
    // is expected to quote UI element names like "Submit" button; any such mention absent from
    // the interaction event log (BugReelEventLogger's own gesture stream) is ungrounded.
    private fun checkUiElementsGrounded(context: ReportContext, output: AiOutput): String? {
        val text = combinedText(output)
        val mentioned = UI_ELEMENT_PATTERN.findAll(text).map { it.groupValues[1] }.toList()
        if (mentioned.isEmpty()) return null
        val log = context.evidenceBundle.eventLog.orEmpty()
        val ungrounded = mentioned.filterNot { log.contains(it, ignoreCase = true) }
        return if (ungrounded.isNotEmpty()) {
            "References UI element(s) not found in evidence: ${ungrounded.joinToString(", ")}"
        } else {
            null
        }
    }

    // Criterion (b) — claims a severity not supported by the evidence bundle, e.g. CRITICAL
    // with no crash signal anywhere in the logs.
    private fun checkSeverityConsistency(context: ReportContext): String? {
        val severity = context.draft.severity ?: return null
        if (severity != Severity.CRITICAL) return null
        val combinedLog = listOfNotNull(context.evidenceBundle.logText, context.evidenceBundle.eventLog)
            .joinToString(" ")
        val hasCrashSignal = CRASH_KEYWORDS.any { combinedLog.contains(it, ignoreCase = true) }
        return if (!hasCrashSignal) "Severity CRITICAL claimed without crash evidence in the logs" else null
    }

    // Criterion (c) — repro steps not derivable from the BugReelEventLogger gesture log.
    private fun checkReproStepsGrounded(context: ReportContext, output: AiOutput): String? {
        if (output.formattedReproSteps.isEmpty()) return null
        val log = context.evidenceBundle.eventLog
        if (log.isNullOrBlank()) {
            return "Repro steps generated but no interaction event log exists to support them"
        }
        val ungrounded = output.formattedReproSteps.filter { step ->
            val words = step.split(Regex("\\W+")).filter { it.length > MIN_GROUNDING_WORD_LENGTH }
            words.isNotEmpty() && words.none { log.contains(it, ignoreCase = true) }
        }
        return if (ungrounded.isNotEmpty()) {
            "Repro step(s) not derivable from the event log: ${ungrounded.joinToString("; ")}"
        } else {
            null
        }
    }

    // Criterion (d) — states device metadata inconsistent with DeviceProfile.
    private fun checkDeviceMetadataConsistency(context: ReportContext, output: AiOutput): String? {
        val text = combinedText(output)
        val profile = context.deviceProfile
        val mentionedBrand = KNOWN_DEVICE_BRANDS.firstOrNull { text.contains(it, ignoreCase = true) } ?: return null
        val matchesProfile = profile.brand.contains(mentionedBrand, ignoreCase = true) ||
            profile.model.contains(mentionedBrand, ignoreCase = true)
        return if (!matchesProfile) {
            "Generated text mentions \"$mentionedBrand\", inconsistent with the device profile " +
                "(${profile.brand} ${profile.model})"
        } else {
            null
        }
    }

    private fun combinedText(output: AiOutput): String =
        listOfNotNull(output.generatedTitle, output.generatedSummary).joinToString(" ") +
            " " + output.formattedReproSteps.joinToString(" ")

    private companion object {
        val UI_ELEMENT_PATTERN = Regex(
            "\"([^\"]+)\"\\s+(button|icon|menu|tab|screen|dialog|field)",
            RegexOption.IGNORE_CASE
        )
        val CRASH_KEYWORDS = listOf(
            "crash", "fatal exception", "fatal signal", "anr", "force close", "not responding"
        )
        val KNOWN_DEVICE_BRANDS = listOf(
            "Samsung", "Pixel", "iPhone", "Xiaomi", "OnePlus", "Motorola", "Huawei", "Sony", "LG", "Nokia"
        )
        const val MIN_GROUNDING_WORD_LENGTH = 3
    }
}
