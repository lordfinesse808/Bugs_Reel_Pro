package com.bugreel.pro.core.di

import com.bugreel.pro.BuildConfig
import com.bugreel.pro.core.data.remote.ai.AIResult
import com.bugreel.pro.core.data.remote.ai.AIService
import com.bugreel.pro.core.data.remote.ai.SafetyResult
import com.bugreel.pro.core.domain.model.AiOutput
import com.bugreel.pro.core.domain.model.ReportContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().apply {
        if (BuildConfig.DEBUG) {
            addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        }
    }.build()

    @Provides
    @Named("aiBaseUrl")
    fun provideAiBaseUrl(): String = "https://api.openai.com/v1/"

    // CLAUDE.md Section 9 / Anti-Pattern 8 — model routing goes through BuildConfig only,
    // never a hardcoded "gpt-*" string at any call site.
    @Provides
    @Named("aiModelStandard")
    fun provideAiModelStandard(): String = BuildConfig.AI_MODEL_STANDARD

    @Provides
    @Named("aiModelSafety")
    fun provideAiModelSafety(): String = BuildConfig.AI_MODEL_SAFETY

    // CLAUDE.md Section 15 — AIService's real implementation (OpenAIService) is Sprint 4
    // scope. This stub exists purely so the DI graph resolves end-to-end in Sprint 2; any
    // Sprint 2/3 code path that accidentally calls it fails loudly instead of silently.
    @Provides
    @Singleton
    fun provideAiService(): AIService = object : AIService {
        override suspend fun generateTitle(context: ReportContext): AIResult<String> =
            throw NotImplementedError("AIService implemented Sprint 4")

        override suspend fun generateSummary(context: ReportContext): AIResult<String> =
            throw NotImplementedError("AIService implemented Sprint 4")

        override suspend fun formatReproSteps(context: ReportContext): AIResult<List<String>> =
            throw NotImplementedError("AIService implemented Sprint 4")

        override suspend fun validateSafety(
            context: ReportContext,
            output: AiOutput
        ): SafetyResult = throw NotImplementedError("AIService implemented Sprint 4")
    }
}
