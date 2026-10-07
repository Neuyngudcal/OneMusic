# OneMusic Samsung One UI 8.5 ProGuard & R8 Configuration

# 1. KotlinX Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class com.example.onemusic.data.** { *; }

# 2. Media3 ExoPlayer & MediaSession
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.ui.** { *; }
-keep interface androidx.media3.common.Player$* { *; }

# 3. Coil Image Loading
-keep class coil.** { *; }
-dontwarn coil.**

# 4. Compose Runtime & Fast Skip
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# 5. Haze Frosted Glass Shader
-keep class dev.chrisbanes.haze.** { *; }
-dontwarn dev.chrisbanes.haze.**
