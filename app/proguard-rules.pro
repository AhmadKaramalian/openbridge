# OpenBridge ProGuard rules

# Keep all classes in our package (no obfuscation by default for open-source transparency)
-keep class com.openbridge.android.** { *; }

# Keep BuildConfig
-keep class com.openbridge.android.BuildConfig { *; }

# Bluetooth classes
-keep class android.bluetooth.** { *; }
-keep class android.bluetooth.le.** { *; }

# Telecom
-keep class android.telecom.** { *; }

# Default Android rules
-dontwarn android.support.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Compose
-dontwarn androidx.compose.**

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Disable aggressive obfuscation — we WANT the code readable for security audit
-dontobfuscate
-dontshrink
