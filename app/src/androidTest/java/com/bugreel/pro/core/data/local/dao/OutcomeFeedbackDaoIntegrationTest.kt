package com.bugreel.pro.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import com.bugreel.pro.core.data.local.entity.ReportEntity
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// CLAUDE.md Section 1 / Section 23 — THE FLYWHEEL.
@RunWith(AndroidJUnit4::class)
class OutcomeFeedbackDaoIntegrationTest {

    private lateinit var database: BugReelDatabase
    private lateinit var outcomeFeedbackDao: OutcomeFeedbackDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        outcomeFeedbackDao = database.outcomeFeedbackDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // OutcomeFeedbackEntity has a CASCADE foreign key on ReportEntity — a report row must
    // exist first or the insert violates the constraint.
    private suspend fun insertReport(id: String) {
        database.reportDao().insert(
            ReportEntity(
                id = id,
                title = "App crashes on launch",
                summary = "Crashes immediately after the splash screen",
                reproSteps = listOf("Open app", "Wait for splash screen"),
                severity = Severity.CRITICAL,
                frequency = Frequency.ALWAYS,
                platform = TestPlatform.UTEST,
                status = ReportStatus.SUBMITTED,
                createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
                syncedAt = null
            )
        )
    }

    private fun testFeedback(reportId: String) = OutcomeFeedbackEntity(
        reportId = reportId,
        platformOutcome = "ACCEPTED",
        rejectionReason = null,
        reportedByTesterAt = Instant.parse("2026-01-02T00:00:00Z").toEpochMilli(),
        preAiQualityScore = 55,
        postAiQualityScore = 82,
        testerOverrodeSuggestion = true,
        aiSuggestionsAccepted = "[\"title\"]",
        aiSuggestionsRejected = "[\"summary\"]"
    )

    @Test
    fun `insert_and_queryByReportId_returns_correct_entity`() = runTest {
        insertReport("report-1")
        val feedback = testFeedback("report-1")

        outcomeFeedbackDao.insert(feedback)

        assertEquals(feedback, outcomeFeedbackDao.queryByReportId("report-1"))
    }

    @Test
    fun `preAiQualityScore_and_postAiQualityScore_are_persisted_correctly`() = runTest {
        insertReport("report-2")
        outcomeFeedbackDao.insert(testFeedback("report-2"))

        val stored = outcomeFeedbackDao.queryByReportId("report-2")

        assertNotNull(stored)
        assertEquals(55, stored?.preAiQualityScore)
        assertEquals(82, stored?.postAiQualityScore)
    }

    @Test
    fun `getAll_flow_emits_when_new_record_is_inserted`() = runTest {
        insertReport("report-3")

        outcomeFeedbackDao.getAll().test {
            assertEquals(emptyList<OutcomeFeedbackEntity>(), awaitItem())

            outcomeFeedbackDao.insert(testFeedback("report-3"))

            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
