package com.jfaf.calendario.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.jfaf.calendario.resources.Res
import com.jfaf.calendario.resources.nunito
import org.jetbrains.compose.resources.Font
import androidx.compose.ui.unit.dp
import com.jfaf.calendario.domain.model.AppStyle

/** Estilo activo; las pantallas lo leen para decidir cómo dibujar (p. ej. los días del calendario). */
val LocalAppStyle = staticCompositionLocalOf { AppStyle.Pop }

@Composable
fun CalendarioTheme(style: AppStyle, darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppStyle provides style) {
        MaterialTheme(
            colorScheme = colorSchemeFor(style, darkTheme),
            shapes = shapesFor(style),
            typography = typographyFor(style),
            content = content,
        )
    }
}

private fun shapesFor(style: AppStyle): Shapes = when (style) {
    AppStyle.Pop -> Shapes(
        extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(14.dp),
        medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp),
    )
    AppStyle.Sorbete -> Shapes(
        extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(32.dp), extraLarge = RoundedCornerShape(40.dp),
    )
    AppStyle.Neon -> Shapes(
        extraSmall = RoundedCornerShape(6.dp), small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp), extraLarge = RoundedCornerShape(28.dp),
    )
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

private fun colorSchemeFor(style: AppStyle, dark: Boolean): ColorScheme = when (style) {
    AppStyle.Pop -> if (dark) popDark else popLight
    AppStyle.Sorbete -> if (dark) sorbeteDark else sorbeteLight
    AppStyle.Neon -> if (dark) neonDark else neonLight
}

private val popLight = lightColorScheme(
    primary = c(0x7B2FF7), onPrimary = c(0xFFFFFF), primaryContainer = c(0xEBD9FF), onPrimaryContainer = c(0x2A0A66),
    secondary = c(0xE0306E), onSecondary = c(0xFFFFFF), secondaryContainer = c(0xFFD9E6), onSecondaryContainer = c(0x5C0A2A),
    tertiary = c(0x00A37F), onTertiary = c(0xFFFFFF), tertiaryContainer = c(0xBFF5E4), onTertiaryContainer = c(0x00382B),
    background = c(0xFFF1F7), onBackground = c(0x2A1033), surface = c(0xFFF1F7), onSurface = c(0x2A1033),
    surfaceVariant = c(0xF3DDF0), onSurfaceVariant = c(0x6E4A80), outline = c(0x9A78AA), outlineVariant = c(0xE4CCE6),
    surfaceContainerLowest = c(0xFFFFFF), surfaceContainerLow = c(0xFFFFFF), surfaceContainer = c(0xFBE8F4),
    surfaceContainerHigh = c(0xF6E3F5), surfaceContainerHighest = c(0xEFD9EF),
)

private val popDark = darkColorScheme(
    primary = c(0xCDB0FF), onPrimary = c(0x3B0B8C), primaryContainer = c(0x5A1FC4), onPrimaryContainer = c(0xEBD9FF),
    secondary = c(0xFFB0CB), onSecondary = c(0x5C0A2A), secondaryContainer = c(0x86133F), onSecondaryContainer = c(0xFFD9E6),
    tertiary = c(0x6FE9C6), onTertiary = c(0x00382B), tertiaryContainer = c(0x005140), onTertiaryContainer = c(0xBFF5E4),
    background = c(0x1B0F24), onBackground = c(0xF6E8FF), surface = c(0x1B0F24), onSurface = c(0xF6E8FF),
    surfaceVariant = c(0x3A2848), onSurfaceVariant = c(0xCDB5DD), outline = c(0x9A82AA), outlineVariant = c(0x4A3558),
    surfaceContainerLowest = c(0x150A1D), surfaceContainerLow = c(0x241730), surfaceContainer = c(0x2B1D38),
    surfaceContainerHigh = c(0x33223F), surfaceContainerHighest = c(0x3D2B4A),
)

private val sorbeteLight = lightColorScheme(
    primary = c(0x4F6BFF), onPrimary = c(0xFFFFFF), primaryContainer = c(0xDDE5FF), onPrimaryContainer = c(0x0E1F7A),
    secondary = c(0x7C6BFF), onSecondary = c(0xFFFFFF), secondaryContainer = c(0xE8E3FF), onSecondaryContainer = c(0x261B80),
    tertiary = c(0xD9487E), onTertiary = c(0xFFFFFF), tertiaryContainer = c(0xFFD9E6), onTertiaryContainer = c(0x5C0A2A),
    background = c(0xF2F6FF), onBackground = c(0x1B2340), surface = c(0xF2F6FF), onSurface = c(0x1B2340),
    surfaceVariant = c(0xE2E8FA), onSurfaceVariant = c(0x5B6690), outline = c(0x8D97BC), outlineVariant = c(0xD3DAF0),
    surfaceContainerLowest = c(0xFFFFFF), surfaceContainerLow = c(0xFFFFFF), surfaceContainer = c(0xEDF2FF),
    surfaceContainerHigh = c(0xE6EDFF), surfaceContainerHighest = c(0xDDE5FA),
)

private val sorbeteDark = darkColorScheme(
    primary = c(0xB5C3FF), onPrimary = c(0x0E1F7A), primaryContainer = c(0x2F46C9), onPrimaryContainer = c(0xDDE5FF),
    secondary = c(0xC9C0FF), onSecondary = c(0x261B80), secondaryContainer = c(0x40338F), onSecondaryContainer = c(0xE8E3FF),
    tertiary = c(0xFFB0CB), onTertiary = c(0x5C0A2A), tertiaryContainer = c(0x86133F), onTertiaryContainer = c(0xFFD9E6),
    background = c(0x12172B), onBackground = c(0xE4E8FF), surface = c(0x12172B), onSurface = c(0xE4E8FF),
    surfaceVariant = c(0x2B3354), onSurfaceVariant = c(0xA9B3DA), outline = c(0x7B85AB), outlineVariant = c(0x3A4468),
    surfaceContainerLowest = c(0x0D1121), surfaceContainerLow = c(0x1A2038), surfaceContainer = c(0x20284A),
    surfaceContainerHigh = c(0x262E50), surfaceContainerHighest = c(0x30395E),
)

private val neonLight = lightColorScheme(
    primary = c(0x4D7A00), onPrimary = c(0xFFFFFF), primaryContainer = c(0xC6FF3D), onPrimaryContainer = c(0x1D2B00),
    secondary = c(0x3F5BE0), onSecondary = c(0xFFFFFF), secondaryContainer = c(0xDCE3FF), onSecondaryContainer = c(0x0A1A66),
    tertiary = c(0xD2336B), onTertiary = c(0xFFFFFF), tertiaryContainer = c(0xFFD9E6), onTertiaryContainer = c(0x5C0A2A),
    background = c(0xF6F7F0), onBackground = c(0x14160C), surface = c(0xF6F7F0), onSurface = c(0x14160C),
    surfaceVariant = c(0xE4E7D6), onSurfaceVariant = c(0x5A5F48), outline = c(0x8A8F76), outlineVariant = c(0xD3D7C2),
    surfaceContainerLowest = c(0xFFFFFF), surfaceContainerLow = c(0xFFFFFF), surfaceContainer = c(0xEEF0E2),
    surfaceContainerHigh = c(0xE9EBDD), surfaceContainerHighest = c(0xE0E3D2),
)

private val neonDark = darkColorScheme(
    primary = c(0xC6FF3D), onPrimary = c(0x10140A), primaryContainer = c(0x3A5200), onPrimaryContainer = c(0xE6FFA0),
    secondary = c(0x8FA6FF), onSecondary = c(0x0A1A66), secondaryContainer = c(0x2C3D8F), onSecondaryContainer = c(0xDCE3FF),
    tertiary = c(0xFF8FB0), onTertiary = c(0x5C0A2A), tertiaryContainer = c(0x86133F), onTertiaryContainer = c(0xFFD9E6),
    background = c(0x0F0F17), onBackground = c(0xF4F4FA), surface = c(0x0F0F17), onSurface = c(0xF4F4FA),
    surfaceVariant = c(0x2A2A3C), onSurfaceVariant = c(0x8B8BA8), outline = c(0x6C6C88), outlineVariant = c(0x343448),
    surfaceContainerLowest = c(0x0A0A11), surfaceContainerLow = c(0x15151F), surfaceContainer = c(0x1D1D2B),
    surfaceContainerHigh = c(0x262636), surfaceContainerHighest = c(0x303042),
)

/** Nunito (redondeada, fuente variable) en cuatro grosores; el estilo decide cuánto pesan títulos y etiquetas. */
@OptIn(ExperimentalTextApi::class)
@Composable
private fun nunitoFamily(): FontFamily {
    @Composable fun f(w: Int) = Font(Res.font.nunito, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    return FontFamily(f(400), f(600), f(700), f(800))
}

@Composable
private fun typographyFor(style: AppStyle): Typography {
    val family = nunitoFamily()
    val heading = when (style) {
        AppStyle.Pop -> FontWeight.ExtraBold
        AppStyle.Sorbete -> FontWeight.Bold
        AppStyle.Neon -> FontWeight.Bold
    }
    val label = if (style == AppStyle.Neon) FontWeight.SemiBold else FontWeight.Bold
    fun TextStyle.with(w: FontWeight?) = copy(fontFamily = family, fontWeight = w ?: fontWeight)
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.with(heading), displayMedium = d.displayMedium.with(heading),
        displaySmall = d.displaySmall.with(heading),
        headlineLarge = d.headlineLarge.with(heading), headlineMedium = d.headlineMedium.with(heading),
        headlineSmall = d.headlineSmall.with(heading),
        titleLarge = d.titleLarge.with(heading), titleMedium = d.titleMedium.with(FontWeight.Bold),
        titleSmall = d.titleSmall.with(FontWeight.Bold),
        bodyLarge = d.bodyLarge.with(null), bodyMedium = d.bodyMedium.with(null), bodySmall = d.bodySmall.with(null),
        labelLarge = d.labelLarge.with(label), labelMedium = d.labelMedium.with(label),
        labelSmall = d.labelSmall.with(label),
    )
}
