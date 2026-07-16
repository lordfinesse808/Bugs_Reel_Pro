package com.bugreel.pro.core.di

import android.content.Context
import androidx.work.WorkManager
import com.bugreel.pro.core.util.OemCompat
import com.bugreel.pro.feature.capture.service.BubbleMode
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

// PermissionManager is available via its own @Inject constructor + @Singleton scope
// (core/util/PermissionManager.kt) — it is not re-declared here, since a class already
// providing itself via constructor injection plus an explicit @Provides for the same type
// would be a duplicate Hilt binding. OemCompat is a Kotlin object, which Dagger cannot
// constructor-inject, so it needs an explicit @Provides.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideOemCompat(): OemCompat = OemCompat

    // ADR-005 — resolved once at app start; BubbleOverlayManager never switches modes
    // at runtime. Falls back to FALLBACK only once needsPiPFallback() reports the Play
    // Console policy outcome from Sprint 0 Track 2 as RED.
    @Provides
    @Named("bubbleMode")
    fun provideBubbleMode(): BubbleMode =
        if (OemCompat.needsPiPFallback()) BubbleMode.FALLBACK else BubbleMode.PRIMARY

    // CLAUDE.md Section 7 — ReportRepositoryImpl enqueues SyncWorker here after every Room
    // write; WorkManager's own on-demand initializer already wires HiltWorkerFactory
    // (BugReelApplication), so getInstance() resolves @HiltWorker classes correctly.
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)
}
