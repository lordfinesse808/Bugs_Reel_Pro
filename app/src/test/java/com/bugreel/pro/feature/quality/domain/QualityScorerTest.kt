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

class QualityScorerTest {

    private val scorer = QualityScorer()

    @Test
    fun `empty draft scores zero`() {
        val score = scorer.score(emptyDraft())

        assertEquals(0, score.total)
        assertEquals(0, score.reproduction)
        assertEquals(0, score.evidence)
        assertEquals(0, score.description)
        assertEquals(0, score.severity)
        assertEquals(0, score.metadata)
    }

    @Test
    fun `fully complete draft scores 100`() {
        val score = scorer.score(fullDraft())

        assertEquals(100, score.total)
        assertEquals(25, score.reproduction)
        assertEquals(25, score.evidence)
        assertEquals(20, score.description)
        assertEquals(15, score.severity)
        assertEquals(15, score.metadata)
    }

    @Test
    fun `same input always produces same output`() {
        val draft = fullDraft()
        val results = (1..1000).map { scorer.score(draft) }
        assertTrue(results.all { it == results.first() })
    }

    @Test
    fun `every sub-score below 80 percent of max generates a coaching hint`() {
        val score = scorer.score(emptyDraft())

        assertEquals(3, score.coaching.size)
        assertEquals(listOf("reproduction", "evidence", "description"), score.coaching.map { it.field })
    }

    @Test
    fun `coaching hints are sorted by scoreImpact descending and capped at 3`() {
        val score = scorer.score(emptyDraft())

        val impacts = score.coaching.map { it.scoreImpact }
        assertEquals(impacts.sortedDescending(), impacts)
        assertTrue(score.coaching.size <= 3)
    }

    @Test
    fun `no coaching hint for a dimension scoring at or above 80 percent of max`() {
        // reproduction = 20/25 (80% exactly) via 2 short steps: present+count, no clarity bonus.
        val draft = emptyDraft().copy(reproSteps = listOf("short", "steps"))

        val score = scorer.score(draft)

        assertEquals(20, score.reproduction)
        assertTrue(score.coaching.none { it.field == "reproduction" })
    }

    @Test
    fun `coaching hint appears just below the 80 percent threshold`() {
        // Every other dimension maxed out so reproduction's partial credit (15/25, 60%) isn't
        // crowded out of the top-3 coaching cap by dimensions with a larger absolute gap.
        val draft = fullDraft().copy(reproSteps = listOf("this step is long enough"))

        val score = scorer.score(draft)

        assertEquals(15, score.reproduction)
        assertEquals(listOf("reproduction"), score.coaching.map { it.field })
    }

    @Test
    fun `partial evidence gives partial credit not zero`() {
        val draft = emptyDraft().copy(
            evidenceBundle = EvidenceBundle(
                recordingUri = null,
                screenshots = listOf("content://media/screenshot_0.png"),
                logText = null,
                eventLog = null
            )
        )

        val score = scorer.score(draft)

        assertEquals(7, score.evidence)
    }

    @Test
    fun `reproduction requires at least 2 steps for the count bonus`() {
        val draft = emptyDraft().copy(reproSteps = listOf("only one step here"))

        val score = scorer.score(draft)

        assertEquals(15, score.reproduction) // present (10) + clarity (5), no count bonus
    }

    @Test
    fun `description rejects placeholder text even when long enough`() {
        val draft = emptyDraft().copy(summary = "test test test test test test test test test test test.")

        val score = scorer.score(draft)

        assertEquals(15, score.description) // length bonuses only, no no-placeholder bonus
    }

    @Test
    fun `description mentioning testing is not treated as placeholder`() {
        val draft = emptyDraft().copy(
            summary = "The tester reproduced this by tapping submit twice during a slow network test."
        )

        val score = scorer.score(draft)

        assertEquals(20, score.description)
    }

    @Test
    fun `critical severity without strong evidence is inconsistent`() {
        val draft = emptyDraft().copy(severity = Severity.CRITICAL)

        val score = scorer.score(draft)

        assertEquals(10, score.severity) // filled only, not consistent
    }

    @Test
    fun `critical severity with a recording is consistent`() {
        val draft = emptyDraft().copy(
            severity = Severity.CRITICAL,
            evidenceBundle = EvidenceBundle(
                recordingUri = "content://media/recording.mp4",
                screenshots = emptyList(),
                logText = null,
                eventLog = null
            )
        )

        val score = scorer.score(draft)

        assertEquals(15, score.severity)
    }

    @Test
    fun `medium severity is always consistent regardless of evidence`() {
        val draft = emptyDraft().copy(severity = Severity.MEDIUM)

        val score = scorer.score(draft)

        assertEquals(15, score.severity)
    }

    private fun emptyDraft() = BugReportDraft(
        id = "draft-1",
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

    private fun fullDraft() = BugReportDraft(
        id = "draft-2",
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after a cold start of the app.",
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
            logText = "some log output",
            eventLog = null
        ),
        qualityScore = null,
        aiOutput = null,
        platform = TestPlatform.UTEST,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z")
    )
}
