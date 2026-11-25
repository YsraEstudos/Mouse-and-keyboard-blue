# Controle BT — R8 / ProGuard keep rules

# Keep Moshi generated adapters & DTOs
-keep class com.example.network.** { *; }
-keep class com.example.security.VaultCredential { *; }
-keep class com.example.data.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Kotlin Metadata for reflection-based adapters
-keep class kotlin.Metadata { *; }

# Preserve line numbers for clean crash traces
-keepattributes SourceFile,LineNumberTable
