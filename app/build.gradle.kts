import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services) // lee app/google-services.json (fuera de git)
}

// Configuración de Supabase desde local.properties (la clave pública; nunca la service_role)
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.jfaf.calendario"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.jfaf.calendario"
        minSdk = 29
        targetSdk = 36
        versionCode = providers.gradleProperty("app.versionCode").get().toInt()
        versionName = "0.1.0"
        buildConfigField("String", "SUPABASE_URL", "\"${localProps.getProperty("supabase.url", "")}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProps.getProperty("supabase.anonKey", "")}\"")
        // DSN de Sentry (informes de fallos); vacío = desactivado. Se declara en local.properties como `sentry.dsn`.
        buildConfigField("String", "SENTRY_DSN", "\"${localProps.getProperty("sentry.dsn", "")}\"")
    }

    // Clave de subida a Play (fuera de git): se declara en local.properties (ver Calendario-arquitectura/10-distribucion.md).
    // Sin esas propiedades la release sale sin firmar y la compilación no falla.
    val uploadStore = localProps.getProperty("release.storeFile")
    if (uploadStore != null) {
        signingConfigs.create("upload") {
            storeFile = rootProject.file(uploadStore)
            storePassword = localProps.getProperty("release.storePassword")
            keyAlias = localProps.getProperty("release.keyAlias")
            keyPassword = localProps.getProperty("release.keyPassword")
        }
    }

    buildTypes {
        getByName("release") {
            signingConfigs.findByName("upload")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Igual que release (R8 y reglas), firmada con la clave de depuración y con otro id: se instala junto a la
        // app normal sin tocar sus datos y sirve para comprobar que la ofuscación no rompe nada.
        create("releaseCheck") {
            initWith(getByName("release"))
            applicationIdSuffix = ".releasecheck"
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
        }
        // Desarrollo: depurable y con otro id (com.jfaf.calendario.dev), para probar sin tocar la app de producción.
        create("dev") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".dev"
            matchingFallbacks += "debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging) // avisos push (ver supabase/social_push.sql)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen) // pantalla de arranque con el icono (Android 12+ nativa, antes emulada)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.koin.android)
    implementation(libs.play.app.update)
    implementation(libs.sentry.android)
    implementation(libs.androidx.glance.appwidget) // widget de pantalla de inicio (turnos de hoy y mañana)
}

kotlin {
    compilerOptions { jvmTarget = JvmTarget.JVM_17 }
}
