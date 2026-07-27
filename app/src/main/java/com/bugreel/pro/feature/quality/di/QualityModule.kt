package com.bugreel.pro.feature.quality.di

import com.bugreel.pro.core.domain.repository.OutcomeFeedbackRepository
import com.bugreel.pro.feature.quality.data.OutcomeFeedbackRepositoryImpl
import com.bugreel.pro.feature.quality.data.QualityRepositoryImpl
import com.bugreel.pro.feature.quality.domain.QualityRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class QualityModule {

    @Binds
    abstract fun bindQualityRepository(impl: QualityRepositoryImpl): QualityRepository

    @Binds
    abstract fun bindOutcomeFeedbackRepository(impl: OutcomeFeedbackRepositoryImpl): OutcomeFeedbackRepository
}
