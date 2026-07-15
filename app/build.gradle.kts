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
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    // Applied only once google-services.json is present (CLAUDE.md Section 22).
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.bugreel.pro"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bugreel.pro"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "AI_MODEL_STANDARD", "\"gpt-4.1-mini\"")
        buildConfigField("String", "AI_MODEL_SAFETY", "\"gpt-4.1\"")
        buildConfigField("int", "QUALITY_SCORE_UPGRADE", "70")
        buildConfigField("int", "AI_RATE_LIMIT_RPM", "20")
        buildConfigField("int", "SYNC_INTERVAL_MINUTES", "30")
        buildConfigField("boolean", "FLYWHEEL_ENABLED", "true")

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

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(Deps.androidxCoreKtx)
    implementation(platform("androidx.compose:compose-bom:${Versions.composeBom}"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation(Deps.composeNavigation)

    implementation(Deps.hiltAndroid)
    ksp(Deps.hiltCompiler)

    implementation(Deps.roomRuntime)
    implementation(Deps.roomKtx)
    ksp(Deps.roomCompiler)

    implementation(Deps.workManagerKtx)
    implementation(Deps.datastoreCore)
    implementation(Deps.okhttp)
    implementation(Deps.kotlinxSerializationJson)
    implementation(Deps.coilCompose)

    implementation(platform("com.google.firebase:firebase-bom:${Versions.firebaseBom}"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    testImplementation(Deps.mockk)
    testImplementation(Deps.turbine)
    androidTestImplementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:${Versions.uiautomator}")
}
