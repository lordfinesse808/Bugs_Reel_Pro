package com.bugreel.pro.feature.analytics

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bugreel.pro.core.data.OutcomeFeedbackRepositoryImpl
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.entity.ReportEntity
import com.bugreel.pro.core.domain.model.BugReportDraft
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import com.bugreel.pro.core.domain.quality.QualityScorer
import com.bugreel.pro.feature.analytics.domain.RecordOutcomeUseCase
import com.bugreel.pro.feature.report.domain.BuildReportUseCase
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// CLAUDE.md Section 18 item 15 — full flow: submit report -> get outcome -> OutcomeFeedbackDao
// has correct row. Exercises the real Room database plus the real use cases on both ends of the
// flywheel (BuildReportUseCase.recordOutcomeFeedback at submission, RecordOutcomeUseCase at
// tester resolution) with no mocks -- neither Firebase nor AIService is on this path.
@RunWith(AndroidJUnit4::class)
class OutcomeFeedbackEndToEndTest {

    private lateinit var database: BugReelDatabase
    private lateinit var outcomeFeedbackRepository: OutcomeFeedbackRepositoryImpl
    private lateinit var buildReportUseCase: BuildReportUseCase
    private lateinit var recordOutcomeUseCase: RecordOutcomeUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        outcomeFeedbackRepository = OutcomeFeedbackRepositoryImpl(database.outcomeFeedbackDao())
        buildReportUseCase = BuildReportUseCase(QualityScorer(), outcomeFeedbackRepository)
        recordOutcomeUseCase = RecordOutcomeUseCase(outcomeFeedbackRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // OutcomeFeedbackEntity has a CASCADE foreign key on ReportEntity.
    private suspend fun insertReportRow(id: String) {
        database.reportDao().insert(
            ReportEntity(
                id = id,
                title = "Login button unresponsive",
                summary = "Tapping login does nothing on the first attempt after a cold start.",
                reproSteps = listOf("Cold start the app", "Tap the login button"),
                severity = Severity.HIGH,
                frequency = Frequency.SOMETIMES,
                platform = TestPlatform.UTEST,
                status = ReportStatus.SUBMITTED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                syncedAt = null
            )
        )
    }

    private fun completeDraft(id: String) = BugReportDraft(
        id = id,
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after a cold start of the app.",
        reproSteps = listOf("Cold start the app", "Tap the login button"),
        severity = Severity.HIGH,
        frequency = Frequency.SOMETIMES,
        deviceProfile = DeviceProfile("Google", "Pixel 8", "15", 35, 8192L, "1080x2400", "WIFI"),
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

    @Test
    fun `submitting_a_report_writes_a_PENDING_row_then_resolving_to_ACCEPTED_updates_the_same_row`() = runTest {
        insertReportRow("e2e-report-1")
        val report = buildReportUseCase.buildOrNull(completeDraft("e2e-report-1"))
        assertNotNull(report)

        buildReportUseCase.recordOutcomeFeedback(report!!)

        val afterSubmit = database.outcomeFeedbackDao().queryByReportId("e2e-report-1")
        assertNotNull(afterSubmit)
        assertEquals("PENDING", afterSubmit?.platformOutcome)
        assertEquals(report.qualityScore!!.total, afterSubmit?.preAiQualityScore)

        recordOutcomeUseCase("e2e-report-1", PlatformOutcome.ACCEPTED, rejectionReason = null)

        val afterOutcome = database.outcomeFeedbackDao().queryByReportId("e2e-report-1")
        assertNotNull(afterOutcome)
        assertEquals("ACCEPTED", afterOutcome?.platformOutcome)
        assertEquals(report.qualityScore!!.total, afterOutcome?.preAiQualityScore)
    }

    @Test
    fun `resolving_to_REJECTED_persists_the_rejection_reason_on_the_row`() = runTest {
        insertReportRow("e2e-report-2")
        val report = buildReportUseCase.buildOrNull(completeDraft("e2e-report-2"))!!
        buildReportUseCase.recordOutcomeFeedback(report)

        recordOutcomeUseCase("e2e-report-2", PlatformOutcome.REJECTED, rejectionReason = "Missing repro steps")

        val row = database.outcomeFeedbackDao().queryByReportId("e2e-report-2")
        assertEquals("REJECTED", row?.platformOutcome)
        assertEquals("Missing repro steps", row?.rejectionReason)
    }
}
