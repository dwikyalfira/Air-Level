# kotlinx.serialization ships its own consumer rules; keep our @Serializable models' serializers.
-keepattributes *Annotation*, InnerClasses, Signature
-keep,includedescriptorclasses class com.airwear.aqi.data.iqair.**$$serializer { *; }
-keepclassmembers class com.airwear.aqi.data.iqair.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit suspend functions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Reflection-created by WorkManager / App Startup. AGP 9 + R8 stripped these
# (crash: NoSuchMethodException WorkDatabase_Impl.<init>), so keep explicitly.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * implements androidx.startup.Initializer { <init>(); }
