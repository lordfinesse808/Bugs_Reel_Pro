package com.bugreel.pro.core.di

import com.bugreel.pro.core.data.remote.ai.AIService
import com.bugreel.pro.core.data.remote.ai.DefaultPromptRepository
import com.bugreel.pro.core.data.remote.ai.OpenAIService
import com.bugreel.pro.core.data.remote.ai.PromptRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// CLAUDE.md Section 17 — Sprint 4 replaces the Sprint 2 stub AIService (NetworkModule) with the
// real OpenAIService/DefaultPromptRepository pair. Kept as its own @Binds module (rather than
// added to NetworkModule) since NetworkModule is a @Provides object -- @Binds requires an
// abstract class.
@Module
@InstallIn(SingletonComponent::class)
abstract class AiServiceModule {

    @Binds
    abstract fun bindAiService(impl: OpenAIService): AIService

    @Binds
    abstract fun bindPromptRepository(impl: DefaultPromptRepository): PromptRepository
}
