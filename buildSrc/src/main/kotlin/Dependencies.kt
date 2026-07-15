// CLAUDE.md Section 2 / Section 24 — Living Document Protocol.
// These versions are UNVERIFIED PLACEHOLDERS pending human sign-off against the
// official release pages listed in CLAUDE.md Section 2. Do not treat them as current.
// Re-verify before Sprint 1 work begins, then update this file and CLAUDE.md together.

object Versions {
    const val kotlin = "2.1.20"
    const val agp = "8.7.2"
    const val ksp = "2.1.20-1.0.31"
    const val hilt = "2.52"
    const val composeBom = "2025.01.00"
    const val composeNavigation = "2.8.5"
    const val room = "2.7.0"
    const val datastore = "1.1.1"
    const val workManager = "2.10.0"
    const val firebaseBom = "33.7.0"
    const val googleServices = "4.4.2"
    const val crashlyticsPlugin = "3.0.2"
    const val coil = "3.0.4"
    const val kotlinxSerialization = "1.7.3"
    const val okhttp = "4.12.0"
    const val junit5 = "5.11.4"
    const val junit5Plugin = "1.11.2.0"
    const val mockk = "1.13.14"
    const val turbine = "1.2.0"
    const val espresso = "3.6.1"
    const val uiautomator = "2.3.0"
}

object Deps {
    const val androidxCoreKtx = "androidx.core:core-ktx:1.15.0"
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    const val workManagerKtx = "androidx.work:work-runtime-ktx:${Versions.workManager}"
    const val datastoreCore = "androidx.datastore:datastore:${Versions.datastore}"
    const val composeNavigation = "androidx.navigation:navigation-compose:${Versions.composeNavigation}"
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}"
    const val coilCompose = "io.coil-kt.coil3:coil-compose:${Versions.coil}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"
    const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
}
