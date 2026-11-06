# Keep Retrofit / Moshi
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.airwear.aqi.data.iqair.** { *; }
-keep class com.squareup.moshi.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
