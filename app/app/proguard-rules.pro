# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,allowobfuscation,allowshrinking class * {
    <fields>;
}

# Media3 & ExoPlayer
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.exoplayer.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
