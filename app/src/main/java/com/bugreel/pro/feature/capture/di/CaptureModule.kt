package com.bugreel.pro.feature.capture.di

import com.bugreel.pro.feature.capture.data.CaptureRepositoryImpl
import com.bugreel.pro.feature.capture.domain.CaptureRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    abstract fun bindCaptureRepository(impl: CaptureRepositoryImpl): CaptureRepository
}
