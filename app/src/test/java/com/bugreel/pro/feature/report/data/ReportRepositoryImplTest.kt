package com.bugreel.pro.feature.report.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import app.cash.turbine.test
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.domain.model.BugReport
import com.bugreel.pro.core.domain.model.CoachingHint
import com.bugreel.pro.core.domain.model.DeviceProfile
import com.bugreel.pro.core.domain.model.EvidenceBundle
import com.bugreel.pro.core.domain.model.QualityScore
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// CLAUDE.md Section 12 — never mock Room; Robolectric runs a real in-memory database on the
// JVM so this test exercises ReportRepositoryImpl's actual mapping/transaction logic, not a
// stand-in for it. WorkManager is mocked deliberately: this verifies the repository schedules
// a sync, not WorkManager's own scheduling behaviour (that's SyncWorkerIntegrationTest's job).
@RunWith(RobolectricTestRunner::class)
class ReportRepositoryImplTest {

    private lateinit var database: BugReelDatabase
    private lateinit var workManager: WorkManager
    private lateinit var repository: ReportRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workManager = mockk(relaxed = true)
        repository = ReportRepositoryImpl(
            database = database,
            reportDao = database.reportDao(),
            evidenceDao = database.evidenceDao(),
            deviceProfileDao = database.deviceProfileDao(),
            qualityScoreDao = database.qualityScoreDao(),
            aiOutputDao = database.aiOutputDao(),
            workManager = workManager
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insertReport stores report and schedules sync`() = runTest {
        val report = testReport(id = "report-1")

        repository.insertReport(report)

        repository.getAllReports().test {
            val reports = awaitItem()
            assertEquals(1, reports.size)
            assertEquals("report-1", reports.first().id)
            cancelAndIgnoreRemainingEvents()
        }
        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>()) }
    }

    @Test
    fun `insertReport round trips quality score coaching hints as json`() = runTest {
        val hints = listOf(CoachingHint(field = "reproSteps", message = "Add more detail", scoreImpact = 5))
        val report = testReport(id = "report-2").copy(
            qualityScore = QualityScore(
                total = 80,
                reproduction = 20,
                evidence = 20,
                description = 15,
                severity = 15,
                metadata = 10,
                coaching = hints
            )
        )

        repository.insertReport(report)

        repository.getReportById("report-2").test {
            assertEquals(hints, awaitItem()?.qualityScore?.coaching)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `deleteReport removes report from getAllReports`() = runTest {
        repository.insertReport(testReport(id = "report-3"))

        repository.deleteReport("report-3")

        repository.getAllReports().test {
            assertEquals(emptyList<BugReport>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getReportById returns null for unknown id`() = runTest {
        repository.getReportById("does-not-exist").test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun testReport(id: String) = BugReport(
        id = id,
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on the first attempt after cold start.",
        reproSteps = listOf("Cold start app", "Tap login"),
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
        status = ReportStatus.SUBMITTED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        syncedAt = null,
        outcomeFeedback = null
    )
}
