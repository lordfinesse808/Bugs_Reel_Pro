package com.bugreel.pro.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BugReelDatabaseTest {

    private lateinit var database: BugReelDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, BugReelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `database opens and all 7 DAOs are non-null`() {
        assertNotNull(database.reportDao())
        assertNotNull(database.evidenceDao())
        assertNotNull(database.draftDao())
        assertNotNull(database.qualityScoreDao())
        assertNotNull(database.aiOutputDao())
        assertNotNull(database.deviceProfileDao())
        assertNotNull(database.outcomeFeedbackDao())
    }

    @Test
    fun `OutcomeFeedbackDao is accessible from database instance`() {
        assertNotNull(database.outcomeFeedbackDao())
    }
}
