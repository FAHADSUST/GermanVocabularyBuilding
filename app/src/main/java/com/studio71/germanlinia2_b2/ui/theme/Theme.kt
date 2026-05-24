package com.studio71.germanlinia2_b2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.studio71.germanlinia2_b2.data.settings.ThemePreset

private val LightColors = lightColorScheme(
    primary = Color(0xFF3F51B5),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFEF6C00)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF4DB6AC),
    tertiary = Color(0xFFFFB74D)
)

private val MintLightColors = lightColorScheme(
    primary = Color(0xFF006D5B),
    secondary = Color(0xFF2D6A4F),
    tertiary = Color(0xFF4E5D94)
)

private val EyeCareLightColors = lightColorScheme(
    primary = Color(0xFF5D6B3F),
    secondary = Color(0xFF6D7A52),
    tertiary = Color(0xFF7A6D52),
    background = Color(0xFFF7F4E8),
    surface = Color(0xFFF7F4E8)
)

private val LavenderLightColors = lightColorScheme(
    primary = Color(0xFF6C4AB6),
    secondary = Color(0xFF8A63D2),
    tertiary = Color(0xFF3F6FB2)
)

private val PeachLightColors = lightColorScheme(
    primary = Color(0xFFB84C2A),
    secondary = Color(0xFF9A5A00),
    tertiary = Color(0xFFA3446A)
)

private val OceanLightColors = lightColorScheme(
    primary = Color(0xFF006A6B),
    secondary = Color(0xFF2E6273),
    tertiary = Color(0xFF725188)
)

private val OceanDarkColors = darkColorScheme(
    primary = Color(0xFF4FD8DA),
    secondary = Color(0xFF9ACBDD),
    tertiary = Color(0xFFDABCF5)
)

private val SunsetLightColors = lightColorScheme(
    primary = Color(0xFF9A3412),
    secondary = Color(0xFF7C4D00),
    tertiary = Color(0xFF8B3A62)
)

private val SunsetDarkColors = darkColorScheme(
    primary = Color(0xFFFFB59F),
    secondary = Color(0xFFFFB951),
    tertiary = Color(0xFFFFB0CB)
)

private val ForestLightColors = lightColorScheme(
    primary = Color(0xFF2F6B2F),
    secondary = Color(0xFF356859),
    tertiary = Color(0xFF6B5D2F)
)

private val ForestDarkColors = darkColorScheme(
    primary = Color(0xFFA8D5A2),
    secondary = Color(0xFF8BC4B4),
    tertiary = Color(0xFFD8C78A)
)

private val AmoledDarkColors = darkColorScheme(
    primary = Color(0xFF82B1FF),
    secondary = Color(0xFF64FFDA),
    tertiary = Color(0xFFFFAB91),
    background = Color(0xFF000000),
    surface = Color(0xFF000000)
)

@Composable
fun GermanVocabTheme(
    themePreset: ThemePreset = ThemePreset.SYSTEM,
    customPrimaryHex: String = "#3F51B5",
    customSecondaryHex: String = "#00897B",
    customTertiaryHex: String = "#EF6C00",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreset) {
        ThemePreset.SYSTEM, ThemePreset.OCEAN, ThemePreset.SUNSET, ThemePreset.FOREST, ThemePreset.CUSTOM -> isSystemInDarkTheme()
        ThemePreset.LIGHT, ThemePreset.LIGHT_EYE_CARE, ThemePreset.LIGHT_MINT, ThemePreset.LIGHT_LAVENDER, ThemePreset.LIGHT_PEACH -> false
        ThemePreset.DARK, ThemePreset.AMOLED -> true
    }

    val customPrimary = colorFromHexOrFallback(customPrimaryHex, LightColors.primary)
    val customSecondary = colorFromHexOrFallback(customSecondaryHex, LightColors.secondary)
    val customTertiary = colorFromHexOrFallback(customTertiaryHex, LightColors.tertiary)

    val colors = when (themePreset) {
        ThemePreset.SYSTEM, ThemePreset.LIGHT, ThemePreset.DARK -> {
            if (darkTheme) DarkColors else LightColors
        }

        ThemePreset.LIGHT_EYE_CARE -> EyeCareLightColors
        ThemePreset.LIGHT_MINT -> MintLightColors
        ThemePreset.LIGHT_LAVENDER -> LavenderLightColors
        ThemePreset.LIGHT_PEACH -> PeachLightColors
        ThemePreset.OCEAN -> if (darkTheme) OceanDarkColors else OceanLightColors
        ThemePreset.SUNSET -> if (darkTheme) SunsetDarkColors else SunsetLightColors
        ThemePreset.FOREST -> if (darkTheme) ForestDarkColors else ForestLightColors
        ThemePreset.AMOLED -> AmoledDarkColors
        ThemePreset.CUSTOM -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = customPrimary,
                    secondary = customSecondary,
                    tertiary = customTertiary
                )
            } else {
                lightColorScheme(
                    primary = customPrimary,
                    secondary = customSecondary,
                    tertiary = customTertiary
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

private fun colorFromHexOrFallback(rawHex: String, fallback: Color): Color {
    val normalized = rawHex.trim().removePrefix("#")
    val argb = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return fallback
    }
    val parsed = argb.toLongOrNull(16) ?: return fallback
    return Color(parsed.toInt())
}

