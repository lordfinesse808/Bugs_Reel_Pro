package com.bugreel.pro.core.di

import com.bugreel.pro.core.data.OutcomeFeedbackRepositoryImpl
import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Bindings for repository interfaces with no single owning feature (CLAUDE.md Section 23 —
// OutcomeFeedback is written by feature/report and will be read by feature/analytics in
// Sprint 5). Feature-owned repositories (ReportRepository, QualityRepository, ...) are bound in
// their own feature's di module instead.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindOutcomeFeedbackRepository(impl: OutcomeFeedbackRepositoryImpl): OutcomeFeedbackRepository
}
