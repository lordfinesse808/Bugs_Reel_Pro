package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DuplicateDetectorTest {

    private val detector = DuplicateDetector()

    // CLAUDE.md Section 16 — 10 clearly distinct existing reports must never be flagged as
    // duplicates of a new, unrelated candidate.
    @Test
    fun `10 non-duplicates return no false positives`() {
        val existing = (1..10).map { index ->
            report(
                id = "report-$index",
                title = DISTINCT_TITLES[index - 1],
                summary = DISTINCT_SUMMARIES[index - 1]
            )
        }
        val candidate = draft(
            title = "Settings screen dark mode toggle is missing",
            summary = "The settings screen has no way to switch between light and dark themes."
        )

        val duplicates = detector.findDuplicates(candidate, existing)

        assertTrue(duplicates.isEmpty())
    }

    @Test
    fun `near identical title and summary is flagged as duplicate`() {
        val existing = listOf(
            report(
                id = "report-1",
                title = "Login button unresponsive on cold start",
                summary = "Tapping login does nothing on the first attempt after a cold start of the app."
            )
        )
        val candidate = draft(
            title = "Login button is unresponsive on a cold start",
            summary = "Tapping the login button does nothing on the first attempt after a cold start."
        )

        val duplicates = detector.findDuplicates(candidate, existing)

        assertEquals(1, duplicates.size)
        assertEquals("report-1", duplicates.first().id)
    }

    @Test
    fun `candidate with blank title and summary finds no duplicates`() {
        val existing = listOf(report(id = "report-1", title = "Crash on launch", summary = "App crashes immediately."))
        val candidate = draft(title = null, summary = null)

        assertTrue(detector.findDuplicates(candidate, existing).isEmpty())
    }

    @Test
    fun `empty existing report list finds no duplicates`() {
        val candidate = draft(title = "Crash on launch", summary = "App crashes immediately on cold start.")

        assertTrue(detector.findDuplicates(candidate, emptyList()).isEmpty())
    }

    private fun draft(title: String?, summary: String?) = BugReportDraft(
        id = "candidate",
        title = title,
        summary = summary,
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

    private fun report(id: String, title: String, summary: String) = BugReport(
        id = id,
        title = title,
        summary = summary,
        reproSteps = listOf("Step one", "Step two"),
        severity = Severity.MEDIUM,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
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

    private companion object {
        val DISTINCT_TITLES = listOf(
            "Settings screen crashes on rotation",
            "Push notifications arrive twice",
            "Dark mode ignores system font scale",
            "Search results paginate incorrectly",
            "Profile photo upload times out",
            "Offline mode loses draft comments",
            "Video playback stutters on Wi-Fi",
            "Export to PDF omits the last page",
            "Calendar sync drops recurring events",
            "Biometric login fails after app update"
        )
        val DISTINCT_SUMMARIES = listOf(
            "Rotating the device while on the settings screen throws the app into a crash loop.",
            "Push notifications for new messages are delivered twice within a few seconds of each other.",
            "Enabling dark mode does not respect the system-wide font scale accessibility setting.",
            "Scrolling past the second page of search results shows duplicate entries from page one.",
            "Uploading a new profile photo over a slow connection times out without an error message.",
            "Comments drafted while offline disappear entirely once connectivity is restored.",
            "Recorded video playback stutters every few seconds even on a strong Wi-Fi connection.",
            "Exporting a long report to PDF silently drops the final page of content.",
            "Recurring calendar events stop syncing after the third occurrence in a series.",
            "Biometric login stops working for all users immediately after updating the app."
        )
    }
}
