package com.bugreel.pro.core.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.entity.DraftEntity
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DraftDaoIntegrationTest {

    private lateinit var database: BugReelDatabase
    private lateinit var draftDao: DraftDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        draftDao = database.draftDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `insert_draft_and_retrieve_by_id_returns_same_entity`() = runTest {
        val draft = testDraft(id = "draft-1")

        draftDao.upsert(draft)

        assertEquals(draft, draftDao.getById("draft-1"))
    }

    @Test
    fun `update_draft_reflects_updated_fields_on_next_read`() = runTest {
        val draft = testDraft(id = "draft-2")
        draftDao.upsert(draft)

        draftDao.upsert(draft.copy(title = "Updated title"))

        assertEquals("Updated title", draftDao.getById("draft-2")?.title)
    }

    @Test
    fun `delete_draft_returns_null_on_subsequent_getById`() = runTest {
        val draft = testDraft(id = "draft-3")
        draftDao.upsert(draft)

        draftDao.deleteById("draft-3")

        assertNull(draftDao.getById("draft-3"))
    }

    @Test
    fun `getAllDrafts_flow_emits_when_draft_is_inserted`() = runTest {
        draftDao.getAllDrafts().test {
            assertEquals(emptyList<DraftEntity>(), awaitItem())

            draftDao.upsert(testDraft(id = "draft-4"))

            assertEquals(1, awaitItem().size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun testDraft(id: String) = DraftEntity(
        id = id,
        title = "Login button unresponsive",
        summary = null,
        reproSteps = null,
        severity = null,
        frequency = null,
        platform = null,
        deviceProfileJson = null,
        evidenceBundleJson = null,
        qualityScoreJson = null,
        aiOutputJson = null,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        updatedAt = null
    )
}
