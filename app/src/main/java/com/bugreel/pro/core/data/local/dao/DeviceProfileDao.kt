package com.bugreel.pro.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bugreel.pro.core.data.local.entity.DeviceProfileEntity

@Dao
interface DeviceProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deviceProfile: DeviceProfileEntity)

    @Query("SELECT * FROM device_profiles WHERE reportId = :reportId")
    suspend fun getByReportId(reportId: String): DeviceProfileEntity?
}
