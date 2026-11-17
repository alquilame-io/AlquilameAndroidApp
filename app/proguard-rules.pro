# kotlinx.serialization: keep generated serializers of the API models.
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers class com.io.alquilame.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.io.alquilame.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit keeps generic signatures of suspend service methods.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
