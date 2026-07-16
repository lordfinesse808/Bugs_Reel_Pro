package com.bugreel.pro.feature.report.di

import com.bugreel.pro.core.domain.repository.ReportRepository
import com.bugreel.pro.feature.report.data.PlatformTemplateRepositoryImpl
import com.bugreel.pro.feature.report.data.ReportRepositoryImpl
import com.bugreel.pro.feature.report.domain.PlatformTemplateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportModule {

    @Binds
    abstract fun bindReportRepository(impl: ReportRepositoryImpl): ReportRepository

    @Binds
    abstract fun bindPlatformTemplateRepository(
        impl: PlatformTemplateRepositoryImpl
    ): PlatformTemplateRepository
}
