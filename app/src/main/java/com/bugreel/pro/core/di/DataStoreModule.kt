package com.bugreel.pro.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.bugreel.pro.core.data.ConsentRepositoryImpl
import com.bugreel.pro.core.data.local.onboarding.ConsentPreferences
import com.bugreel.pro.core.data.local.onboarding.ConsentPreferencesSerializer
import com.bugreel.pro.core.domain.repository.ConsentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreProvidesModule {

    @Provides
    @Singleton
    fun provideConsentPreferencesDataStore(@ApplicationContext context: Context): DataStore<ConsentPreferences> =
        DataStoreFactory.create(
            serializer = ConsentPreferencesSerializer,
            produceFile = { File(context.filesDir, "datastore/consent_preferences.pb") }
        )
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreBindsModule {

    @Binds
    abstract fun bindConsentRepository(impl: ConsentRepositoryImpl): ConsentRepository
}
