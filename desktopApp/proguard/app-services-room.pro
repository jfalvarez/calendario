# Reglas propias de la app de escritorio para ProGuard (release).
# Mantienen las clases que las librerías cargan con ServiceLoader (META-INF/services),
# porque ProGuard no las ve referenciadas en el código y las elimina.

-keep class dev.whyoleg.cryptography.providers.jdk.JdkCryptographyProviderContainer { <init>(); }
-keep class kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsLoaderImpl { <init>(); }
-keep class kotlin.reflect.jvm.internal.impl.km.jvm.internal.JvmMetadataExtensions { <init>(); }
-keep class kotlin.reflect.jvm.internal.impl.load.java.FieldOverridabilityCondition { <init>(); }
-keep class kotlin.reflect.jvm.internal.impl.load.java.ErasedOverridabilityCondition { <init>(); }
-keep class kotlin.reflect.jvm.internal.impl.load.java.JavaIncompatibilityRulesOverridabilityCondition { <init>(); }
-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { <init>(); }
-keep class io.ktor.client.engine.okhttp.OkHttpEngineContainer { <init>(); }
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider { <init>(); }
-keep class io.ktor.server.config.HoconConfigLoader { <init>(); }

# Referencias a clases opcionales que no están en el runtime de escritorio (no se usan en la app).
# Kotlin 2.4 (kotlin.concurrent.atomics, kotlin.uuid) está en el runtime, pero ProGuard no lo ve como librería.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn org.graalvm.**
-dontwarn com.oracle.**
-dontwarn android.**
-dontwarn kotlin.concurrent.atomics.**
-dontwarn kotlin.uuid.**
-dontwarn kotlin.jvm.internal.EnhancedNullability

# Okio (usada por OkHttp y Supabase): la optimización de ProGuard rompe Okio.buffer y da VerifyError en las peticiones de red.
-keep class okio.** { *; }

# Room genera las clases *_Impl y las crea por reflexión al arrancar.
-keep class * extends androidx.room.RoomDatabase
-keep class **_Impl { *; }
