package com.bugreel.pro.feature.analytics.di

import com.bugreel.pro.feature.analytics.data.AnalyticsRepositoryImpl
import com.bugreel.pro.feature.analytics.domain.AnalyticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    abstract fun bindAnalyticsRepository(impl: AnalyticsRepositoryImpl): AnalyticsRepository
}
