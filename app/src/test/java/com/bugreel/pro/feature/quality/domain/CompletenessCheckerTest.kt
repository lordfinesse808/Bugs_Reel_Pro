package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CompletenessCheckerTest {

    private val checker = CompletenessChecker()

    @Test
    fun `fully complete draft has no missing fields`() {
        assertTrue(checker.missingFields(completeDraft()).isEmpty())
        assertTrue(checker.isComplete(completeDraft()))
    }

    @Test
    fun `missing title is caught independently`() {
        assertEquals(listOf("title"), checker.missingFields(completeDraft().copy(title = null)))
    }

    @Test
    fun `blank title is caught independently`() {
        assertEquals(listOf("title"), checker.missingFields(completeDraft().copy(title = "   ")))
    }

    @Test
    fun `missing summary is caught independently`() {
        assertEquals(listOf("summary"), checker.missingFields(completeDraft().copy(summary = null)))
    }

    @Test
    fun `missing repro steps is caught independently`() {
        assertEquals(listOf("reproSteps"), checker.missingFields(completeDraft().copy(reproSteps = null)))
    }

    @Test
    fun `empty repro steps list is caught independently`() {
        assertEquals(listOf("reproSteps"), checker.missingFields(completeDraft().copy(reproSteps = emptyList())))
    }

    @Test
    fun `missing severity is caught independently`() {
        assertEquals(listOf("severity"), checker.missingFields(completeDraft().copy(severity = null)))
    }

    @Test
    fun `missing frequency is caught independently`() {
        assertEquals(listOf("frequency"), checker.missingFields(completeDraft().copy(frequency = null)))
    }

    @Test
    fun `missing device profile is caught independently`() {
        assertEquals(listOf("deviceProfile"), checker.missingFields(completeDraft().copy(deviceProfile = null)))
    }

    @Test
    fun `missing evidence bundle is caught independently`() {
        assertEquals(listOf("evidence"), checker.missingFields(completeDraft().copy(evidenceBundle = null)))
    }

    @Test
    fun `evidence bundle with no recording or screenshots is caught independently`() {
        val draft = completeDraft().copy(
            evidenceBundle = EvidenceBundle(recordingUri = null, screenshots = emptyList(), logText = null, eventLog = null)
        )
        assertEquals(listOf("evidence"), checker.missingFields(draft))
    }

    @Test
    fun `missing platform is caught independently`() {
        assertEquals(listOf("platform"), checker.missingFields(completeDraft().copy(platform = null)))
    }

    @Test
    fun `multiple missing fields are all reported`() {
        val draft = completeDraft().copy(title = null, severity = null, platform = null)
        assertEquals(listOf("title", "severity", "platform"), checker.missingFields(draft))
    }

    @Test
    fun `all fields missing reports every field`() {
        val draft = BugReportDraft(
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
        )
        assertEquals(
            listOf("title", "summary", "reproSteps", "severity", "frequency", "deviceProfile", "evidence", "platform"),
            checker.missingFields(draft)
        )
        assertTrue(!checker.isComplete(draft))
    }

    private fun completeDraft() = BugReportDraft(
        id = "draft-1",
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after a cold start.",
        reproSteps = listOf("Cold start the app", "Tap the login button"),
        severity = Severity.HIGH,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile(
            brand = "Google",
            model = "Pixel 8",
            osVersion = "15",
            apiLevel = 35,
            ramMb = 8192L,
            screenResolution = "1080x2400",
            networkType = "WIFI"
        ),
        evidenceBundle = EvidenceBundle(
            recordingUri = "content://media/recording.mp4",
            screenshots = listOf("content://media/screenshot_0.png"),
            logText = null,
            eventLog = null
        ),
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.UTEST,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
