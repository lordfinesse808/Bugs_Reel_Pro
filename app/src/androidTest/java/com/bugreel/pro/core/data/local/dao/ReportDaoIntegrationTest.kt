package com.bugreel.pro.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.entity.ReportEntity
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportDaoIntegrationTest {

    private lateinit var database: BugReelDatabase
    private lateinit var reportDao: ReportDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reportDao = database.reportDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert report and get by id returns same entity`() = runTest {
        val report = testReport(id = "report-1")

        reportDao.insert(report)

        assertEquals(report, reportDao.getById("report-1"))
    }

    @Test
    fun `update report reflects updated fields on next read`() = runTest {
        val report = testReport(id = "report-2")
        reportDao.insert(report)

        reportDao.update(report.copy(title = "Updated title"))

        assertEquals("Updated title", reportDao.getById("report-2")?.title)
    }

    @Test
    fun `delete report returns null on subsequent getById`() = runTest {
        val report = testReport(id = "report-3")
        reportDao.insert(report)

        reportDao.delete(report)

        assertNull(reportDao.getById("report-3"))
    }

    @Test
    fun `getUnsyncedReports returns only reports with null syncedAt`() = runTest {
        reportDao.insert(testReport(id = "unsynced"))
        reportDao.insert(testReport(id = "synced").copy(syncedAt = Instant.parse("2026-01-02T00:00:00Z")))

        val unsynced = reportDao.getUnsyncedReports()

        assertEquals(listOf("unsynced"), unsynced.map { it.id })
    }

    @Test
    fun `markSynced sets syncedAt so report no longer appears as unsynced`() = runTest {
        reportDao.insert(testReport(id = "report-4"))

        reportDao.markSynced("report-4", Instant.parse("2026-01-03T00:00:00Z"))

        assertEquals(emptyList<String>(), reportDao.getUnsyncedReports().map { it.id })
    }

    @Test
    fun `getAllReports flow emits when report is inserted`() = runTest {
        reportDao.getAllReports().test {
            assertEquals(emptyList<ReportEntity>(), awaitItem())

            reportDao.insert(testReport(id = "report-5"))

            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById emits the updated entity after update`() = runTest {
        val report = testReport(id = "report-6")
        reportDao.insert(report)

        reportDao.observeById("report-6").test {
            assertEquals("Login button unresponsive", awaitItem()?.title)

            reportDao.update(report.copy(title = "Updated title"))

            assertEquals("Updated title", awaitItem()?.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun testReport(id: String) = ReportEntity(
        id = id,
        title = "Login button unresponsive",
        summary = "Tapping login does nothing on cold start.",
        reproSteps = listOf("Cold start app", "Tap login"),
        severity = Severity.HIGH,
        frequency = Frequency.SOMETIMES,
        platform = TestPlatform.UTEST,
        status = ReportStatus.SUBMITTED,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        syncedAt = null
    )
}
