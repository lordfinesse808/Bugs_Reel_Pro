package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bugreel.pro.core.data.local.entity.QualityScoreEntity

@Dao
interface QualityScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(qualityScore: QualityScoreEntity)

    @Query("SELECT * FROM quality_scores WHERE reportId = :reportId")
    suspend fun getByReportId(reportId: String): QualityScoreEntity?
}
