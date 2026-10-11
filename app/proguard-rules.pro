# BugReel Pro release (R8) keep rules. Hilt, Room, WorkManager, Compose, OkHttp and Firebase ship
# their own consumer rules; only what this app does reflectively needs listing here.

# Crashlytics: keep line numbers so release stack traces are readable after deobfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization — @Serializable DTOs (OpenAI request/response DTOs in
# core/data/remote/ai/dto, SyncMapper's private wire DTOs) are looked up via generated
# $serializer companions.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.bugreel.pro.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.bugreel.pro.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.bugreel.pro.**$$serializer { *; }

# DataStore consent preferences serializer reads/writes ConsentPreferences via kotlinx.serialization.
-keep class com.bugreel.pro.core.data.local.onboarding.** { *; }

# Room enum columns are stored by name (Converters.kt uses Enum.name / valueOf).
-keepclassmembers enum com.bugreel.pro.core.domain.model.enums.** { *; }
