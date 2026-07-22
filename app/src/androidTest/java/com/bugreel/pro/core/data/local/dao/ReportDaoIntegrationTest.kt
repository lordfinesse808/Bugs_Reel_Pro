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
    fun `insert_report_and_get_by_id_returns_same_entity`() = runTest {
        val report = testReport(id = "report-1")

        reportDao.insert(report)

        assertEquals(report, reportDao.getById("report-1"))
    }

    @Test
    fun `update_report_reflects_updated_fields_on_next_read`() = runTest {
        val report = testReport(id = "report-2")
        reportDao.insert(report)

        reportDao.update(report.copy(title = "Updated title"))

        assertEquals("Updated title", reportDao.getById("report-2")?.title)
    }

    @Test
    fun `delete_report_returns_null_on_subsequent_getById`() = runTest {
        val report = testReport(id = "report-3")
        reportDao.insert(report)

        reportDao.delete(report)

        assertNull(reportDao.getById("report-3"))
    }

    @Test
    fun `getUnsyncedReports_returns_only_reports_with_null_syncedAt`() = runTest {
        reportDao.insert(testReport(id = "unsynced"))
        reportDao.insert(testReport(id = "synced").copy(syncedAt = Instant.parse("2026-01-02T00:00:00Z")))

        val unsynced = reportDao.getUnsyncedReports()

        assertEquals(listOf("unsynced"), unsynced.map { it.id })
    }

    @Test
    fun `markSynced_sets_syncedAt_so_report_no_longer_appears_as_unsynced`() = runTest {
        reportDao.insert(testReport(id = "report-4"))

        reportDao.markSynced("report-4", Instant.parse("2026-01-03T00:00:00Z"))

        assertEquals(emptyList<String>(), reportDao.getUnsyncedReports().map { it.id })
    }

    @Test
    fun `getAllReports_flow_emits_when_report_is_inserted`() = runTest {
        reportDao.getAllReports().test {
            assertEquals(emptyList<ReportEntity>(), awaitItem())

            reportDao.insert(testReport(id = "report-5"))

            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeById_emits_the_updated_entity_after_update`() = runTest {
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
