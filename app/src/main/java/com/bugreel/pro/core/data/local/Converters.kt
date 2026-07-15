package com.bugreel.pro.core.data.local

import androidx.room.TypeConverter
import com.bugreel.pro.core.domain.model.enums.Frequency
import com.bugreel.pro.core.domain.model.enums.PlatformOutcome
import com.bugreel.pro.core.domain.model.enums.ReportStatus
import com.bugreel.pro.core.domain.model.enums.Severity
import com.bugreel.pro.core.domain.model.enums.TestPlatform
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant

class Converters {

    // Instant <-> Long
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    // List<String> <-> JSON
    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.let { Json.encodeToString(it) }

    @TypeConverter
    fun toStringList(value: String?): List<String>? = value?.let { Json.decodeFromString(it) }

    // Severity <-> String
    @TypeConverter
    fun fromSeverity(value: Severity?): String? = value?.name

    @TypeConverter
    fun toSeverity(value: String?): Severity? = value?.let { Severity.valueOf(it) }

    // Frequency <-> String
    @TypeConverter
    fun fromFrequency(value: Frequency?): String? = value?.name

    @TypeConverter
    fun toFrequency(value: String?): Frequency? = value?.let { Frequency.valueOf(it) }

    // ReportStatus <-> String
    @TypeConverter
    fun fromReportStatus(value: ReportStatus?): String? = value?.name

    @TypeConverter
    fun toReportStatus(value: String?): ReportStatus? = value?.let { ReportStatus.valueOf(it) }

    // PlatformOutcome <-> String
    @TypeConverter
    fun fromPlatformOutcome(value: PlatformOutcome?): String? = value?.name

    @TypeConverter
    fun toPlatformOutcome(value: String?): PlatformOutcome? = value?.let { PlatformOutcome.valueOf(it) }

    // TestPlatform <-> String
    @TypeConverter
    fun fromTestPlatform(value: TestPlatform?): String? = value?.name

    @TypeConverter
    fun toTestPlatform(value: String?): TestPlatform? = value?.let { TestPlatform.valueOf(it) }
}
