package com.bugreel.pro.core.di

import com.bugreel.pro.BuildConfig
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
}
