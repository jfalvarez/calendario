import java.util.Properties
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.jfaf.calendario.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "NurseOClock"
            packageVersion = "1.0.0"
            description = "Calendario de turnos"
            vendor = "jfaf"
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            windows {
                menuGroup = "NurseOClock"
                iconFile.set(project.file("icons/icon.ico"))
                shortcut = true
                perUserInstall = true
                // Identifica la app en las actualizaciones del MSI: no cambiar nunca
                upgradeUuid = "6b0f6f0e-3c1a-4c52-9d0e-2a7d6a1c9e41"
            }
        }
    }
}

// Escritorio: empaqueta el `versionCode` común (gradle.properties) para compararlo con app_config en Supabase.
val appVersionCode = providers.gradleProperty("app.versionCode").get()
val appVersionName = "1.0.0" // mantener igual que packageVersion (arriba)
val generateAppVersion by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/appversion")
    inputs.property("versionCode", appVersionCode)
    inputs.property("versionName", appVersionName)
    outputs.dir(outDir)
    doLast {
        val file = File(outDir.get().asFile, "app-version.properties").apply { parentFile.mkdirs() }
        file.writeText("version.code=$appVersionCode\nversion.name=$appVersionName\n")
    }
}

// Escritorio: lee la configuración de Supabase de local.properties y la empaqueta como recurso.
val generateSupabaseConfig by tasks.registering {
    val outDir = layout.buildDirectory.dir("generated/supabase")
    val propsFile = rootProject.file("local.properties")
    inputs.files(propsFile).optional()
    outputs.dir(outDir)
    doLast {
        val props = Properties().apply { if (propsFile.exists()) propsFile.inputStream().use { load(it) } }
        val file = File(outDir.get().asFile, "supabase.properties").apply { parentFile.mkdirs() }
        file.writeText(
            "supabase.url=${props.getProperty("supabase.url", "")}\n" +
                "supabase.anonKey=${props.getProperty("supabase.anonKey", "")}\n"
        )
    }
}
tasks.processResources { from(generateSupabaseConfig, generateAppVersion) }
