package com.bugreel.pro.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "device_profiles",
    foreignKeys = [
        ForeignKey(
            entity = ReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("reportId")]
)
data class DeviceProfileEntity(
    @PrimaryKey val reportId: String,
    val brand: String,
    val model: String,
    val osVersion: String,
    val apiLevel: Int,
    val ramMb: Long,
    val screenResolution: String,
    val networkType: String
)
