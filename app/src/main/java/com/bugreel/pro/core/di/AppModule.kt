package com.bugreel.pro.core.di

import com.bugreel.pro.core.util.OemCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// PermissionManager is available via its own @Inject constructor + @Singleton scope
// (core/util/PermissionManager.kt) — it is not re-declared here, since a class already
// providing itself via constructor injection plus an explicit @Provides for the same type
// would be a duplicate Hilt binding. OemCompat is a Kotlin object, which Dagger cannot
// constructor-inject, so it needs an explicit @Provides.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideOemCompat(): OemCompat = OemCompat
}
