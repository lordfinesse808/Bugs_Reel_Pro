package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bugreel.pro.core.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Update
    suspend fun update(report: ReportEntity)

    @Delete
    suspend fun delete(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE id = :id")
    suspend fun getById(id: String): ReportEntity?

    @Query("SELECT * FROM reports")
    fun getAllReports(): Flow<List<ReportEntity>>

    // CLAUDE.md Section 7 — SyncWorker's push set; syncedAt IS NULL means Room hasn't
    // confirmed a successful Firestore write for that report yet.
    @Query("SELECT * FROM reports WHERE syncedAt IS NULL")
    suspend fun getUnsyncedReports(): List<ReportEntity>

    @Query("UPDATE reports SET syncedAt = :syncedAt WHERE id = :id")
    suspend fun markSynced(id: String, syncedAt: Instant)
}
