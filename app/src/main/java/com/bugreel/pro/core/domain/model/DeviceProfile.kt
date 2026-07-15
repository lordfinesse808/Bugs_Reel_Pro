package com.bugreel.pro.core.domain.model

data class DeviceProfile(
    val brand: String,
    val model: String,
    val osVersion: String,
    val apiLevel: Int,
    val ramMb: Long,
    val screenResolution: String,
    val networkType: String
)
