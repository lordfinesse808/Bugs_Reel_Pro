package com.bugreel.pro.feature.quality.domain

import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// CLAUDE.md Section 16 — >=25 test cases covering all 5 severity recommendations across a
// spread of evidence scenarios (crash signals, recording+screenshots, either alone, log-only,
// no evidence, and edge-case combinations/casing).
class SeverityAdvisorTest {

    private val advisor = SeverityAdvisor()

    @Test
    fun `null evidence recommends cosmetic`() {
        assertEquals(Severity.COSMETIC, advisor.recommend(null))
    }

    @Test
    fun `completely empty evidence recommends cosmetic`() {
        assertEquals(Severity.COSMETIC, advisor.recommend(evidence()))
    }

    @Test
    fun `blank strings and empty lists recommend cosmetic`() {
        assertEquals(
            Severity.COSMETIC,
            advisor.recommend(evidence(recordingUri = "", logText = "", eventLog = ""))
        )
    }

    @Test
    fun `log text only recommends low`() {
        assertEquals(Severity.LOW, advisor.recommend(evidence(logText = "some ordinary log output")))
    }

    @Test
    fun `event log only recommends low`() {
        assertEquals(Severity.LOW, advisor.recommend(evidence(eventLog = "tap login; tap submit")))
    }

    @Test
    fun `both log text and event log without crash signal recommends low`() {
        assertEquals(
            Severity.LOW,
            advisor.recommend(evidence(logText = "info: ok", eventLog = "tap login"))
        )
    }

    @Test
    fun `recording only recommends medium`() {
        assertEquals(Severity.MEDIUM, advisor.recommend(evidence(recordingUri = "content://recording.mp4")))
    }

    @Test
    fun `screenshots only recommends medium`() {
        assertEquals(Severity.MEDIUM, advisor.recommend(evidence(screenshots = listOf("s1.png"))))
    }

    @Test
    fun `multiple screenshots without recording still recommends medium`() {
        assertEquals(
            Severity.MEDIUM,
            advisor.recommend(evidence(screenshots = listOf("s1.png", "s2.png", "s3.png")))
        )
    }

    @Test
    fun `recording and screenshots together recommend high`() {
        assertEquals(
            Severity.HIGH,
            advisor.recommend(evidence(recordingUri = "content://recording.mp4", screenshots = listOf("s1.png")))
        )
    }

    @Test
    fun `recording, screenshots, and log text together without crash signal recommend high`() {
        assertEquals(
            Severity.HIGH,
            advisor.recommend(
                evidence(
                    recordingUri = "content://recording.mp4",
                    screenshots = listOf("s1.png"),
                    logText = "info: ok"
                )
            )
        )
    }

    @Test
    fun `crash keyword in log text recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "app crash detected")))
    }

    @Test
    fun `fatal exception keyword recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "FATAL EXCEPTION: main")))
    }

    @Test
    fun `fatal signal keyword recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(eventLog = "Fatal signal 11 (SIGSEGV)")))
    }

    @Test
    fun `anr keyword recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "ANR in com.bugreel.pro")))
    }

    @Test
    fun `force close keyword recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "app force close observed")))
    }

    @Test
    fun `not responding keyword recommends critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(eventLog = "app is not responding")))
    }

    @Test
    fun `crash keyword is case insensitive`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "CRASH REPORT ATTACHED")))
    }

    @Test
    fun `crash keyword takes priority over recording and screenshots`() {
        assertEquals(
            Severity.CRITICAL,
            advisor.recommend(
                evidence(
                    recordingUri = "content://recording.mp4",
                    screenshots = listOf("s1.png"),
                    logText = "crash"
                )
            )
        )
    }

    @Test
    fun `crash keyword takes priority over recording alone`() {
        assertEquals(
            Severity.CRITICAL,
            advisor.recommend(evidence(recordingUri = "content://recording.mp4", eventLog = "crash"))
        )
    }

    @Test
    fun `crash keyword takes priority over screenshots alone`() {
        assertEquals(
            Severity.CRITICAL,
            advisor.recommend(evidence(screenshots = listOf("s1.png"), logText = "crash"))
        )
    }

    @Test
    fun `crash keyword takes priority over log-only low case`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "normal log, then a crash")))
    }

    @Test
    fun `recording present but blank screenshots list recommends medium not high`() {
        assertEquals(
            Severity.MEDIUM,
            advisor.recommend(evidence(recordingUri = "content://recording.mp4", screenshots = emptyList()))
        )
    }

    @Test
    fun `blank recording uri with screenshots recommends medium`() {
        assertEquals(Severity.MEDIUM, advisor.recommend(evidence(recordingUri = "", screenshots = listOf("s1.png"))))
    }

    @Test
    fun `blank recording uri with no other evidence recommends cosmetic`() {
        assertEquals(Severity.COSMETIC, advisor.recommend(evidence(recordingUri = "")))
    }

    @Test
    fun `blank log text with no other evidence recommends cosmetic`() {
        assertEquals(Severity.COSMETIC, advisor.recommend(evidence(logText = "")))
    }

    @Test
    fun `word containing crash as substring still triggers critical`() {
        assertEquals(Severity.CRITICAL, advisor.recommend(evidence(logText = "server crashed at 10:02am")))
    }

    @Test
    fun `all evidence types present with crash signal still recommends critical`() {
        assertEquals(
            Severity.CRITICAL,
            advisor.recommend(
                evidence(
                    recordingUri = "content://recording.mp4",
                    screenshots = listOf("s1.png"),
                    logText = "crash",
                    eventLog = "tap login"
                )
            )
        )
    }

    private fun evidence(
        recordingUri: String? = null,
        screenshots: List<String> = emptyList(),
        logText: String? = null,
        eventLog: String? = null
    ) = EvidenceBundle(
        recordingUri = recordingUri,
        screenshots = screenshots,
        logText = logText,
        eventLog = eventLog
    )
}
