// CLAUDE.md Section 2 / Section 24 — Living Document Protocol.
// These versions are UNVERIFIED PLACEHOLDERS pending human sign-off against the
// official release pages listed in CLAUDE.md Section 2. Do not treat them as current.
// Re-verify before Sprint 1 work begins, then update this file and CLAUDE.md together.

object Versions {
    // Language / build tooling
    const val kotlin = "2.1.20"
    const val agp = "8.7.2"
    const val ksp = "2.1.20-1.0.31"

    // DI
    // 2.52 cannot read Kotlin 2.1.x class metadata (hiltJavaCompileDebug throws
    // "Unable to read Kotlin metadata due to unsupported metadata version" on any
    // @Inject-annotated Kotlin class — see github.com/google/dagger/issues/4451).
    // 2.53.1 is the minimal fix: bumps kotlin-metadata-jvm to 2.0.21.
    const val hilt = "2.53.1"

    // UI
    const val composeBom = "2026.06.00"
    const val composeNavigation = "2.8.5"

    // Persistence
    const val room = "2.7.0"
    const val datastore = "1.1.1"

    // Background
    const val workManager = "2.10.0"

    // Firebase — firebaseBom pins the individual Firestore/Auth/Storage/Crashlytics
    // artifact versions referenced in CLAUDE.md Section 2; do not version them separately.
    const val firebaseBom = "33.7.0"
    const val googleServices = "4.4.2"
    const val crashlyticsPlugin = "3.0.2"

    // Networking / serialization
    const val okhttp = "4.12.0"
    const val kotlinxSerialization = "1.7.3"

    // Images
    const val coil = "3.0.4"

    // Testing
    const val junit5 = "5.11.4"
    const val junit5Plugin = "1.11.2.0"
    const val mockk = "1.13.14"
    const val turbine = "1.2.0"
    const val espresso = "3.6.1"
    const val uiautomator = "2.3.0"
}

object Libs {
    // AndroidX core
    const val androidxCoreKtx = "androidx.core:core-ktx:1.15.0"

    // Compose — ui/material3 versions resolved via the compose-bom platform
    const val composeUi = "androidx.compose.ui:ui"
    const val composeMaterial3 = "androidx.compose.material3:material3"
    const val composeNavigation = "androidx.navigation:navigation-compose:${Versions.composeNavigation}"

    // DI
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"

    // Persistence
    const val roomRuntime = "androidx.room:room-runtime:${Versions.room}"
    const val roomKtx = "androidx.room:room-ktx:${Versions.room}"
    const val roomCompiler = "androidx.room:room-compiler:${Versions.room}"
    const val datastoreCore = "androidx.datastore:datastore:${Versions.datastore}"

    // Background
    const val workManagerKtx = "androidx.work:work-runtime-ktx:${Versions.workManager}"

    // Firebase — versions resolved via the firebase-bom platform
    const val firebaseFirestore = "com.google.firebase:firebase-firestore-ktx"
    const val firebaseAuth = "com.google.firebase:firebase-auth-ktx"
    const val firebaseStorage = "com.google.firebase:firebase-storage-ktx"
    const val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics-ktx"

    // Networking / serialization
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.okhttp}"
    const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}"

    // Images
    const val coilCompose = "io.coil-kt.coil3:coil-compose:${Versions.coil}"

    // Testing
    const val junit5Api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit5}"
    const val junit5Engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"
    const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
    const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    const val uiautomator = "androidx.test.uiautomator:uiautomator:${Versions.uiautomator}"
}
