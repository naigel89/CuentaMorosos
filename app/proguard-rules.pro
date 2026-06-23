# ProGuard / R8 Rules — CuentaMorosos
# Security hardening: preserves critical runtime classes for release builds.

# --- General ---
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions,InnerClasses,EnclosingMethod

# --- Firebase Auth ---
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.auth.internal.** { *; }
-keep class com.google.android.gms.internal.firebase-auth-api.** { *; }
-dontwarn com.google.firebase.auth.**

# --- Firebase Firestore ---
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firebase.firestore.proto.** { *; }
-keep class com.google.firestore.** { *; }
-keep class io.grpc.** { *; }
-dontwarn com.google.firebase.firestore.**
-dontwarn com.google.firestore.**

# --- Firebase Storage ---
-keep class com.google.firebase.storage.** { *; }
-dontwarn com.google.firebase.storage.**

# --- Firebase Messaging (FCM) ---
-keep class com.google.firebase.messaging.** { *; }
-dontwarn com.google.firebase.messaging.**

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Preserve serializers for CuentaMorosos data classes
-keep,includedescriptorclasses class com.cuentamorosos.**$$serializer { *; }
-keepclassmembers class com.cuentamorosos.** {
    *** Companion;
}
-keepclasseswithmembers class com.cuentamorosos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Jetpack Compose ---
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** {
    <init>(...);
}
-dontwarn androidx.compose.**

# --- SQLDelight ---
-keep class app.cash.sqldelight.** { *; }
-keep class com.cuentamorosos.db.** { *; }
-keep class com.cuentamorosos.database.** { *; }
-dontwarn app.cash.sqldelight.**

# --- GitLive Firebase (KMP) ---
-keep class dev.gitlive.firebase.** { *; }
-keep class dev.gitlive.firebase.firestore.** { *; }
-keep class dev.gitlive.firebase.auth.** { *; }
-dontwarn dev.gitlive.firebase.**

# --- AndroidX (runtime dependencies) ---
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
