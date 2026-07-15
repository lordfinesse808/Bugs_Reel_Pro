// Root build file. Version numbers live in buildSrc/src/main/kotlin/Dependencies.kt (CLAUDE.md Section 2).
// Re-verify these against official release pages before each sprint (CLAUDE.md Section 24).

plugins {
    id("com.android.application") version Versions.agp apply false
    id("com.android.library") version Versions.agp apply false
    kotlin("android") version Versions.kotlin apply false
    kotlin("plugin.serialization") version Versions.kotlin apply false
    id("com.google.devtools.ksp") version Versions.ksp apply false
    id("com.google.dagger.hilt.android") version Versions.hilt apply false
    id("com.google.gms.google-services") version Versions.googleServices apply false
    id("com.google.firebase.crashlytics") version Versions.crashlyticsPlugin apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
