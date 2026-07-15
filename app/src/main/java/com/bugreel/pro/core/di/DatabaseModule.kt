package com.bugreel.pro.core.di

import android.content.Context
import androidx.room.Room
import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.data.local.BugReelDatabase
import com.bugreel.pro.core.data.local.dao.AiOutputDao
import com.bugreel.pro.core.data.local.dao.DeviceProfileDao
import com.bugreel.pro.core.data.local.dao.DraftDao
import com.bugreel.pro.core.data.local.dao.EvidenceDao
import com.bugreel.pro.core.data.local.dao.OutcomeFeedbackDao
import com.bugreel.pro.core.data.local.dao.QualityScoreDao
import com.bugreel.pro.core.data.local.dao.ReportDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBugReelDatabase(@ApplicationContext context: Context): BugReelDatabase {
        val builder = Room.databaseBuilder(
            context,
            BugReelDatabase::class.java,
            "bugreel.db"
        )
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration(true)
        }
        return builder.build()
    }

    @Provides
    fun provideReportDao(database: BugReelDatabase): ReportDao = database.reportDao()

    @Provides
    fun provideEvidenceDao(database: BugReelDatabase): EvidenceDao = database.evidenceDao()

    @Provides
    fun provideDraftDao(database: BugReelDatabase): DraftDao = database.draftDao()

    @Provides
    fun provideQualityScoreDao(database: BugReelDatabase): QualityScoreDao =
        database.qualityScoreDao()

    @Provides
    fun provideAiOutputDao(database: BugReelDatabase): AiOutputDao = database.aiOutputDao()

    @Provides
    fun provideDeviceProfileDao(database: BugReelDatabase): DeviceProfileDao =
        database.deviceProfileDao()

    @Provides
    fun provideOutcomeFeedbackDao(database: BugReelDatabase): OutcomeFeedbackDao =
        database.outcomeFeedbackDao()
}
