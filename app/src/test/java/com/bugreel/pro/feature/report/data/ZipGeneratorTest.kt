package com.bugreel.pro.feature.report.data

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.io.File
import java.time.Instant
import java.util.Collections
import java.util.zip.ZipFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// CLAUDE.md Section 15 stop condition — "ZIP bundle structure verified by unzipping a
// generated ZIP in a test."
@RunWith(RobolectricTestRunner::class)
class ZipGeneratorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val zipGenerator = ZipGenerator(context)

    @Test
    fun `generateZip produces the fixed recording, screenshots, log and report json structure`() {
        val recordingFile = File.createTempFile("recording", ".mp4").apply { writeBytes(ByteArray(16)) }
        val screenshotFile = File.createTempFile("screenshot", ".png").apply { writeBytes(ByteArray(16)) }
        val evidence = EvidenceBundle(
            recordingUri = Uri.fromFile(recordingFile).toString(),
            screenshots = listOf(Uri.fromFile(screenshotFile).toString()),
            logText = "line one\nline two",
            eventLog = null
        )

        val result = zipGenerator.generateZip(testReport(), evidence)

        assertTrue(result.isSuccess)
        val entryNames = ZipFile(result.getOrThrow()).use { zip -> Collections.list(zip.entries()).map { it.name } }
        assertEquals(
            setOf("recording.mp4", "screenshots/screenshot_0.png", "log.txt", "report.json"),
            entryNames.toSet()
        )
    }

    @Test
    fun `generateZip report json entry contains the report title`() {
        val report = testReport()
        val evidence = EvidenceBundle(recordingUri = null, screenshots = emptyList(), logText = null, eventLog = null)

        val result = zipGenerator.generateZip(report, evidence)

        val reportJsonText = ZipFile(result.getOrThrow()).use { zip ->
            zip.getInputStream(zip.getEntry("report.json")).bufferedReader().use { it.readText() }
        }
        assertTrue(reportJsonText.contains(report.title))
    }

    @Test
    fun `generateZip with no evidence still produces log txt and report json`() {
        val evidence = EvidenceBundle(recordingUri = null, screenshots = emptyList(), logText = null, eventLog = null)

        val result = zipGenerator.generateZip(testReport(), evidence)

        assertTrue(result.isSuccess)
        val entryNames = ZipFile(result.getOrThrow()).use { zip -> Collections.list(zip.entries()).map { it.name } }
        assertEquals(setOf("log.txt", "report.json"), entryNames.toSet())
    }

    private fun testReport() = BugReport(
        id = "report-zip-1",
        title = "Crash on submit",
        summary = "App crashes when tapping submit with an empty title field.",
        reproSteps = listOf("Open report builder", "Leave title empty", "Tap submit"),
        severity = Severity.CRITICAL,
        frequency = Frequency.ALWAYS,
        deviceProfile = DeviceProfile(
            brand = "Google",
            model = "Pixel 8",
            osVersion = "15",
            apiLevel = 35,
            ramMb = 8192L,
            screenResolution = "1080x2400",
            networkType = "WIFI"
        ),
        evidenceBundle = EvidenceBundle(null, emptyList(), null, null),
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.UTEST,
        status = ReportStatus.SUBMITTED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        syncedAt = null,
        outcomeFeedback = null
    )
}
