package com.bugreel.pro.core.data.remote.ai

import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.ReportContext
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultPromptRepositoryTest {

    private val promptRepository = DefaultPromptRepository()
    private val context = validContext()

    @Test
    fun `titlePrompt returns a non-empty string`() {
        assertTrue(promptRepository.titlePrompt(context).isNotBlank())
    }

    @Test
    fun `summaryPrompt returns a non-empty string`() {
        assertTrue(promptRepository.summaryPrompt(context).isNotBlank())
    }

    @Test
    fun `reproStepsPrompt returns a non-empty string`() {
        assertTrue(promptRepository.reproStepsPrompt(context).isNotBlank())
    }

    @Test
    fun `safetyValidationPrompt returns a non-empty string`() {
        val output = AiOutput(
            generatedTitle = "Login button unresponsive",
            generatedSummary = "Tapping login does nothing.",
            formattedReproSteps = listOf("Tap login"),
            safetyPassed = true,
            blockedReason = null
        )
        assertTrue(promptRepository.safetyValidationPrompt(context, output).isNotBlank())
    }

    @Test
    fun `all four prompts stay non-empty even for a mostly-empty draft`() {
        val emptyContext = ReportContext(
            draft = BugReportDraft(
                id = "draft-empty",
                title = null,
                summary = null,
                reproSteps = null,
                severity = null,
                frequency = null,
                deviceProfile = null,
                evidenceBundle = null,
                qualityScore = null,
                aiOutput = null,
                platform = null,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = null
            ),
            deviceProfile = DeviceProfile("", "", "", 0, 0L, "", ""),
            evidenceBundle = EvidenceBundle(null, emptyList(), null, null)
        )
        val emptyOutput = AiOutput(null, null, emptyList(), safetyPassed = false, blockedReason = null)

        assertTrue(promptRepository.titlePrompt(emptyContext).isNotBlank())
        assertTrue(promptRepository.summaryPrompt(emptyContext).isNotBlank())
        assertTrue(promptRepository.reproStepsPrompt(emptyContext).isNotBlank())
        assertTrue(promptRepository.safetyValidationPrompt(emptyContext, emptyOutput).isNotBlank())
    }

    private fun validContext() = ReportContext(
        draft = BugReportDraft(
            id = "draft-1",
            title = "Login button unresponsive",
            summary = "Tapping login does nothing on the first attempt.",
            reproSteps = listOf("Cold start the app", "Tap the login button"),
            severity = Severity.HIGH,
            frequency = Frequency.SOMETIMES,
            deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
            evidenceBundle = EvidenceBundle(
                recordingUri = "content://media/recording.mp4",
                screenshots = listOf("content://media/screenshot_0.png"),
                logText = null,
                eventLog = "tap login button"
            ),
            qualityScore = null,
            aiOutput = null,
            platform = TestPlatform.UTEST,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z")
        ),
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
        evidenceBundle = EvidenceBundle(
            recordingUri = "content://media/recording.mp4",
            screenshots = listOf("content://media/screenshot_0.png"),
            logText = null,
            eventLog = "tap login button"
        )
    )
}
