import java.util.Properties

// CLAUDE.md Section 22 — local.properties supplies secrets; never commit real values.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("de.mannodermaus.android-junit5")
    id("androidx.room")
    jacoco
}

android {
    namespace = "com.bugreel.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bugreel.pro"
        // CLAUDE.md Section 2 — bumped from 28 to 29 (Android 10) 2026-07-17 as a reasonable
        // modernization on its own merits. NOTE: this bump does NOT fix (and was never able to
        // fix) the D8 "Space characters in SimpleName ... not allowed prior to DEX version 040"
        // error hit by backtick androidTest names containing spaces. Root cause (confirmed
        // 2026-07-22 by direct dexBuilderDebugAndroidTest reproduction): kotlinx-coroutines-test's
        // runTest {} (and Turbine's .test {}) are non-inline, so every `= runTest { ... }` body
        // compiles to a real synthetic/continuation class whose name embeds the literal enclosing
        // backtick test method name — spaces and all (visible directly in the .class file name on
        // disk). D8 rejects any DEX SimpleName containing a space below DEX version 040, and no
        // minSdk this project would realistically ship at gets D8 to select version 040 with our
        // AGP 8.7.2 / R8 8.7.18 toolchain. minSdk is NOT the right lever for this — raising it
        // further only trades away device compatibility for crowdtesters without fixing anything.
        // Actual fix (see CLAUDE.md Section 5): androidTest backtick names use underscores instead
        // of spaces (e.g. `insert_draft_and_retrieve_by_id_returns_same_entity`) — hyphens are
        // fine, only literal spaces trip D8. androidTest deliberately stays on JUnit4 (see
        // buildSrc/Dependencies.kt comment); JUnit5 + @DisplayName was considered but rejected to
        // avoid reversing that decision.
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // CLAUDE.md Section 2 / Section 22 — BuildConfig constants
        buildConfigField("String", "AI_MODEL_STANDARD", "\"gpt-4.1-mini\"")
        buildConfigField("String", "AI_MODEL_SAFETY", "\"gpt-4.1\"")
        buildConfigField("int", "QUALITY_SCORE_UPGRADE", "70")
        buildConfigField("int", "AI_RATE_LIMIT_RPM", "20")
        buildConfigField("int", "SYNC_INTERVAL_MINUTES", "30")
        buildConfigField("boolean", "FLYWHEEL_ENABLED", "true")
        // CLAUDE.md Section 13 Track 2 / ADR-005 — set to true if the Play Console rejects the
        // SYSTEM_ALERT_WINDOW bubble; every device then uses the PiP/notification-tile trigger.
        buildConfigField("boolean", "BUBBLE_FORCE_FALLBACK", "false")

        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${localProperties.getProperty("OPENAI_API_KEY", "")}\""
        )
        buildConfigField(
            "String",
            "OPENAI_ORG_ID",
            "\"${localProperties.getProperty("OPENAI_ORG_ID", "")}\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // CLAUDE.md Section 22 — release signing secrets come from local.properties (gitignored).
    // When they're absent (CI, a fresh clone) the release build is simply left unsigned rather
    // than failing configuration for every other task.
    val releaseKeystorePath = localProperties.getProperty("KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath)
                storePassword = localProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = localProperties.getProperty("KEY_ALIAS")
                keyPassword = localProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources.excludes.add("META-INF/LICENSE*")
    }

    lint {
        // AGP 8.7.2's bundled lint engine crashes (IncompatibleClassChangeError) running the
        // transitively-included androidx.lifecycle NonNullableMutableLiveDataDetector against
        // Kotlin 2.1.20's analysis API. We don't use MutableLiveData anywhere (Section 11 —
        // StateFlow/SharedFlow only), so disabling this check is safe.
        disable += "NullSafeMutableLiveData"
    }
}

// Room schema export (CLAUDE.md Section 7 — exportSchema always true, JSON committed to repo)
room {
    schemaDirectory("$projectDir/../schemas")
}

// Jacoco coverage (CLAUDE.md Section 12 — coverage targets, Section 20 — jacocoTestReport)
jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
        "**/*Test*.*", "android/**/*.*", "**/*_Hilt*.*", "**/Hilt_*.*",
        "**/*_Factory.*", "**/*_MembersInjector.*", "**/di/**"
    )
    val debugTree = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory.get()) { include("jacoco/testDebugUnitTest.exec") }
    )
}

dependencies {
    implementation(Libs.androidxCoreKtx)

    implementation(platform("androidx.compose:compose-bom:${Versions.composeBom}"))
    implementation(Libs.composeUi)
    implementation(Libs.composeMaterial3)
    implementation(Libs.composeNavigation)
    implementation(Libs.activityCompose)
    implementation(Libs.lifecycleRuntimeCompose)
    implementation(Libs.lifecycleViewModelCompose)

    implementation(Libs.hiltAndroid)
    ksp(Libs.hiltCompiler)
    implementation(Libs.hiltNavigationCompose)
    implementation(Libs.hiltWork)
    ksp(Libs.hiltWorkCompiler)

    implementation(Libs.roomRuntime)
    implementation(Libs.roomKtx)
    ksp(Libs.roomCompiler)

    implementation(Libs.workManagerKtx)
    implementation(Libs.datastoreCore)
    implementation(Libs.okhttp)
    implementation(Libs.okhttpLoggingInterceptor)
    implementation(Libs.kotlinxSerializationJson)
    implementation(Libs.kotlinxCoroutinesPlayServices)
    implementation(Libs.coilCompose)

    implementation(platform("com.google.firebase:firebase-bom:${Versions.firebaseBom}"))
    implementation(Libs.firebaseFirestore)
    implementation(Libs.firebaseAuth)
    implementation(Libs.firebaseStorage)
    implementation(Libs.firebaseCrashlytics)

    testImplementation(Libs.junit5Api)
    testRuntimeOnly(Libs.junit5Engine)
    testImplementation(Libs.mockk)
    testImplementation(Libs.turbine)
    testImplementation(Libs.kotlinxCoroutinesTest)
    testImplementation(Libs.mockWebServer)

    // Robolectric has no JUnit5 extension; DeviceProfilerTest runs as a JUnit4 test through
    // the Vintage engine registered above, alongside the Jupiter (JUnit5) tests in this module.
    testImplementation(Libs.junit4)
    testRuntimeOnly(Libs.junit5VintageEngine)
    testImplementation(Libs.robolectric)
    testImplementation(Libs.androidxTestCore)

    androidTestImplementation(Libs.espresso)
    androidTestImplementation(Libs.uiautomator)
    androidTestImplementation(Libs.androidxTestExtJunit)
    androidTestImplementation(Libs.turbine)
    androidTestImplementation(Libs.kotlinxCoroutinesTest)
    androidTestImplementation(Libs.workTesting)
    androidTestImplementation(Libs.mockkAndroid)
}
