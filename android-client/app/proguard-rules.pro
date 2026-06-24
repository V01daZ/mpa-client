# MPA ProGuard rules

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Наши data-классы
-keep class dev.mpa.client.data.ServerProfile { *; }
-keep class dev.mpa.client.data.SourceType { *; }

# sing-box libbox (пакет libbox, не io.nekohasekai)
-keep class libbox.** { *; }
-keep interface libbox.** { *; }
-keep class go.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Coroutines
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
