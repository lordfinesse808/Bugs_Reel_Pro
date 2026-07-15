package com.bugreel.pro.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bugreel.pro.core.data.local.dao.AiOutputDao
import com.bugreel.pro.core.data.local.dao.DeviceProfileDao
import com.bugreel.pro.core.data.local.dao.DraftDao
import com.bugreel.pro.core.data.local.dao.EvidenceDao
import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.dao.QualityScoreDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import com.bugreel.pro.core.data.local.entity.AiOutputEntity
import com.bugreel.pro.core.data.local.entity.DeviceProfileEntity
import com.bugreel.pro.core.data.local.entity.DraftEntity
import com.bugreel.pro.core.data.local.entity.EvidenceEntity
import com.bugreel.pro.core.data.local.entity.OutcomeFeedbackEntity
import com.bugreel.pro.core.data.local.entity.QualityScoreEntity
import com.bugreel.pro.core.data.local.entity.ReportEntity

@Database(
    entities = [
        ReportEntity::class,
        EvidenceEntity::class,
        DraftEntity::class,
        QualityScoreEntity::class,
        AiOutputEntity::class,
        DeviceProfileEntity::class,
        OutcomeFeedbackEntity::class, // ADR-002: Present from version=1. Non-negotiable.
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BugReelDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun evidenceDao(): EvidenceDao
    abstract fun draftDao(): DraftDao
    abstract fun qualityScoreDao(): QualityScoreDao
    abstract fun aiOutputDao(): AiOutputDao
    abstract fun deviceProfileDao(): DeviceProfileDao
    abstract fun outcomeFeedbackDao(): OutcomeFeedbackDao
}
