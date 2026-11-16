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
            packageName = "Calendario"
            packageVersion = "1.0.0"
            description = "Calendario de turnos"
            vendor = "jfaf"
            windows {
                menuGroup = "Calendario"
                shortcut = true
                perUserInstall = true
                // Identifica la app en las actualizaciones del MSI: no cambiar nunca
                upgradeUuid = "6b0f6f0e-3c1a-4c52-9d0e-2a7d6a1c9e41"
            }
        }
    }
}
