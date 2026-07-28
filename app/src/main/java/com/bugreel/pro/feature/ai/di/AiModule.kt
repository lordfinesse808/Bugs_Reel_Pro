package com.bugreel.pro.feature.ai.di

import com.bugreel.pro.feature.ai.data.AiRepositoryImpl
import com.bugreel.pro.feature.ai.domain.AiRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    abstract fun bindAiRepository(impl: AiRepositoryImpl): AiRepository
}
