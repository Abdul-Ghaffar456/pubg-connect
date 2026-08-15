# ProGuard Rules for PUBG Connect
-keep class com.pubgconnect.models.** { *; }
-keepclassmembers class com.pubgconnect.models.** { *; }
-dontwarn com.microsoft.signalr.**
-keep class com.microsoft.signalr.** { *; }
