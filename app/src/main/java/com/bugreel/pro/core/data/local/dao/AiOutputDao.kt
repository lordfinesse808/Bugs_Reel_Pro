package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bugreel.pro.core.data.local.entity.AiOutputEntity

@Dao
interface AiOutputDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(aiOutput: AiOutputEntity)

    @Query("SELECT * FROM ai_outputs WHERE reportId = :reportId")
    suspend fun getByReportId(reportId: String): AiOutputEntity?
}
