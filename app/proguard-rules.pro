# Regras ProGuard/R8 do Lev (Base §2: minify em release).

# --- Room ---
-keep class androidx.room.RoomDatabase { *; }
-keep class com.example.lev.data.model.** { *; }
-keep class com.example.lev.data.local.** { *; }
-dontwarn androidx.room.**

# --- Moshi (modelo via Map<String, Any?> + adapters built-in) ---
-keep class com.squareup.moshi.** { *; }
-keep interface com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# --- OkHttp / Okio ---
-dontwarn okhttp3.**
-dontwarn okio.**

# --- WorkManager (SyncWorker instanciado por reflexão) ---
-keep class com.example.lev.data.work.SyncWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**
