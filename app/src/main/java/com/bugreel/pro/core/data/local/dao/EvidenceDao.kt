package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bugreel.pro.core.data.local.entity.EvidenceEntity

@Dao
interface EvidenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(evidence: EvidenceEntity)

    @Query("SELECT * FROM evidence WHERE reportId = :reportId")
    suspend fun getByReportId(reportId: String): EvidenceEntity?
}
