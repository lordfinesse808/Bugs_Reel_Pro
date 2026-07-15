package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.bugreel.pro.core.data.local.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao {
    @Upsert
    suspend fun upsert(draft: DraftEntity)

    @Query("SELECT * FROM drafts WHERE id = :id")
    suspend fun getById(id: String): DraftEntity?

    @Query("DELETE FROM drafts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM drafts")
    fun getAllDrafts(): Flow<List<DraftEntity>>
}
