package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import kotlinx.coroutines.flow.Flow

// CLAUDE.md Section 1 / Section 23 — THE FLYWHEEL.
@Dao
interface OutcomeFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: OutcomeFeedbackEntity)

    @Query("SELECT * FROM outcome_feedback WHERE reportId = :reportId")
    suspend fun queryByReportId(reportId: String): OutcomeFeedbackEntity?

    @Query("SELECT * FROM outcome_feedback")
    fun getAll(): Flow<List<OutcomeFeedbackEntity>>
}
