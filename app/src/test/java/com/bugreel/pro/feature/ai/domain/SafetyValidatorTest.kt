package com.bugreel.pro.feature.ai.domain

import com.bugreel.pro.core.data.remote.ai.SafetyResult
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.model.enums.Severity
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// CLAUDE.md Section 17 — the 5 required cases: all 4 ADR-004 BLOCKED criteria, plus 1 grounded
// PASS case.
class SafetyValidatorTest {

    private val validator = SafetyValidator()

    @Test
    fun `criterion a - references a UI element absent from evidence is blocked`() {
        val context = context(eventLog = "tap login; tap submit")
        val output = output(generatedSummary = "Tapping the \"Checkout\" button does nothing.")

        val result = validator.validate(context, output)

        assertTrue(result is SafetyResult.Blocked)
        assertTrue((result as SafetyResult.Blocked).reason.contains("Checkout"))
    }

    @Test
    fun `criterion b - critical severity with no crash evidence is blocked`() {
        val context = context(severity = Severity.CRITICAL, logText = "info: user tapped login", eventLog = null)
        val output = output(generatedSummary = "The app misbehaves on login.")

        val result = validator.validate(context, output)

        assertTrue(result is SafetyResult.Blocked)
        assertTrue((result as SafetyResult.Blocked).reason.contains("CRITICAL"))
    }

    @Test
    fun `criterion c - repro step not derivable from event log is blocked`() {
        val context = context(eventLog = "tap login; tap submit")
        val output = output(formattedReproSteps = listOf("Swipe left on the gallery thumbnail carousel"))

        val result = validator.validate(context, output)

        assertTrue(result is SafetyResult.Blocked)
        assertTrue((result as SafetyResult.Blocked).reason.contains("event log"))
    }

    @Test
    fun `criterion d - device metadata inconsistent with DeviceProfile is blocked`() {
        val context = context(brand = "Google", model = "Pixel 8", eventLog = "tap login")
        val output = output(generatedSummary = "Reproduced on a Samsung Galaxy device after cold start.")

        val result = validator.validate(context, output)

        assertTrue(result is SafetyResult.Blocked)
        assertTrue((result as SafetyResult.Blocked).reason.contains("Samsung"))
    }

    @Test
    fun `correct grounded output passes`() {
        val context = context(
            eventLog = "tap login button; tap submit button",
            brand = "Google",
            model = "Pixel 8"
        )
        val output = output(
            generatedTitle = "Login button unresponsive",
            generatedSummary = "Tapping the \"login\" button does nothing on a Pixel device.",
            formattedReproSteps = listOf("Tap the login button", "Tap the submit button")
        )

        val result = validator.validate(context, output)

        assertEquals(SafetyResult.Pass, result)
    }

    private fun context(
        severity: Severity? = Severity.MEDIUM,
        logText: String? = null,
        eventLog: String? = null,
        brand: String = "Google",
        model: String = "Pixel 8"
    ) = ReportContext(
        draft = BugReportDraft(
            id = "draft-1",
            title = null,
            summary = null,
            reproSteps = null,
            severity = severity,
            frequency = null,
            deviceProfile = null,
            evidenceBundle = null,
            qualityScore = null,
            aiOutput = null,
            platform = null,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = null
        ),
        deviceProfile = DeviceProfile(brand, model, "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(
            recordingUri = null,
            screenshots = emptyList(),
            logText = logText,
            eventLog = eventLog
        )
    )

    private fun output(
        generatedTitle: String? = null,
        generatedSummary: String? = null,
        formattedReproSteps: List<String> = emptyList()
    ) = AiOutput(
        generatedTitle = generatedTitle,
        generatedSummary = generatedSummary,
        formattedReproSteps = formattedReproSteps,
        safetyPassed = false,
        blockedReason = null
    )
}
