// CLAUDE.md Section 2 / Section 24 — Living Document Protocol.
// Verified 2026-07-16 against the official release pages in CLAUDE.md Section 2:
//   - composeBom 2026.06.00 IS current (developer.android.com/jetpack/compose/bom).
//   - kotlin 2.1.20 is STALE — latest stable is 2.4.10 (kotlinlang.org/docs/releases.html).
//     Not bumped yet: Kotlin 2.1.x -> 2.4.x also requires re-verifying the KSP build
//     (`ksp`) and the Compose compiler bundled with kotlin("plugin.compose"), and Hilt
//     was already bumped to 2.53.1 once for a Kotlin-2.1.x metadata incompatibility
//     (see the `hilt` comment below) — the same class of break is plausible on this
//     jump. Treat as a tracked upgrade task with a full test/lint re-run, not a
//     same-day doc edit.
object Versions {
    // Language / build tooling
    const val kotlin = "2.1.20" // stale pin — see verification note above
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
    const val activityCompose = "1.9.3"
    const val lifecycle = "2.8.7"
    const val hiltNavigationCompose = "1.2.0"

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
    const val kotlinxCoroutines = "1.9.0"

    // Images
    const val coil = "3.0.4"

    // Testing
    const val junit5 = "5.11.4"
    const val junit5Plugin = "1.11.2.0"
    const val mockk = "1.13.14"
    const val turbine = "1.2.0"
    const val espresso = "3.6.1"
    const val uiautomator = "2.3.0"
    // Robolectric has no native JUnit5 extension; the mannodermaus plugin runs JUnit4-style
    // Robolectric tests through the JUnit Vintage engine alongside Jupiter (JUnit5) tests in
    // the same source set — see de.mannodermaus.android-junit5 docs. androidTest integration
    // tests also stay on JUnit4 (androidx.test.ext:junit) — the AndroidX instrumented test
    // stack (Espresso/UiAutomator/AndroidJUnitRunner) is built around it.
    const val junit4 = "4.13.2"
    const val robolectric = "4.15.1"
    const val androidxTestCore = "1.7.0"
    const val androidxTestExtJunit = "1.3.0"
    // work-testing is version-aligned with work-runtime; mockk-android is MockK's
    // instrumented-test artifact (plain io.mockk:mockk doesn't work on-device).
    const val workTesting = workManager
    const val mockkAndroid = mockk
}

object Libs {
    // AndroidX core
    const val androidxCoreKtx = "androidx.core:core-ktx:1.15.0"

    // Compose — ui/material3 versions resolved via the compose-bom platform
    const val composeUi = "androidx.compose.ui:ui"
    const val composeMaterial3 = "androidx.compose.material3:material3"
    const val composeNavigation = "androidx.navigation:navigation-compose:${Versions.composeNavigation}"
    const val activityCompose = "androidx.activity:activity-compose:${Versions.activityCompose}"
    const val lifecycleRuntimeCompose = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycle}"
    const val lifecycleViewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"

    // DI
    const val hiltAndroid = "com.google.dagger:hilt-android:${Versions.hilt}"
    const val hiltCompiler = "com.google.dagger:hilt-android-compiler:${Versions.hilt}"
    const val hiltNavigationCompose = "androidx.hilt:hilt-navigation-compose:${Versions.hiltNavigationCompose}"
    // hilt-work/hilt-compiler share the androidx.hilt group version with hilt-navigation-compose.
    // Required for @HiltWorker (core/data/sync/SyncWorker) to generate its assisted factory.
    const val hiltWork = "androidx.hilt:hilt-work:${Versions.hiltNavigationCompose}"
    const val hiltWorkCompiler = "androidx.hilt:hilt-compiler:${Versions.hiltNavigationCompose}"

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
    const val okhttpLoggingInterceptor = "com.squareup.okhttp3:logging-interceptor:${Versions.okhttp}"
    const val kotlinxSerializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.kotlinxSerialization}"
    const val kotlinxCoroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.kotlinxCoroutines}"
    // FirebaseFirestore/Storage/Auth's Task-based APIs need this for the .await() extension
    // used throughout FirestoreSource/StorageSource.
    const val kotlinxCoroutinesPlayServices = "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:${Versions.kotlinxCoroutines}"

    // Images
    const val coilCompose = "io.coil-kt.coil3:coil-compose:${Versions.coil}"

    // Testing
    const val junit5Api = "org.junit.jupiter:junit-jupiter-api:${Versions.junit5}"
    const val junit5Engine = "org.junit.jupiter:junit-jupiter-engine:${Versions.junit5}"
    const val mockk = "io.mockk:mockk:${Versions.mockk}"
    const val turbine = "app.cash.turbine:turbine:${Versions.turbine}"
    const val espresso = "androidx.test.espresso:espresso-core:${Versions.espresso}"
    const val uiautomator = "androidx.test.uiautomator:uiautomator:${Versions.uiautomator}"
    const val junit4 = "junit:junit:${Versions.junit4}"
    const val junit5VintageEngine = "org.junit.vintage:junit-vintage-engine:${Versions.junit5}"
    const val robolectric = "org.robolectric:robolectric:${Versions.robolectric}"
    const val androidxTestCore = "androidx.test:core:${Versions.androidxTestCore}"
    const val androidxTestExtJunit = "androidx.test.ext:junit:${Versions.androidxTestExtJunit}"
    const val workTesting = "androidx.work:work-testing:${Versions.workTesting}"
    const val mockkAndroid = "io.mockk:mockk-android:${Versions.mockkAndroid}"
}
