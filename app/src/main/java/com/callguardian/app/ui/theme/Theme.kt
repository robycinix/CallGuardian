package com.callguardian.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callguardian.app.core.model.ThemeMode
import com.callguardian.app.core.model.ThemePalette

private data class PaletteColors(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val darkPrimary: Color,
    val darkSecondary: Color,
    val darkTertiary: Color,
)

private val PaletteMap = mapOf(
    ThemePalette.SECURITY_BLUE to PaletteColors(
        primary = Color(0xFF0057D9),
        secondary = Color(0xFF00A7B7),
        tertiary = Color(0xFFFFB020),
        darkPrimary = Color(0xFF7DB7FF),
        darkSecondary = Color(0xFF41DDE8),
        darkTertiary = Color(0xFFFFD27A),
    ),
    ThemePalette.PROTECTION_GREEN to PaletteColors(
        primary = Color(0xFF007A52),
        secondary = Color(0xFF00A7B7),
        tertiary = Color(0xFFFFB020),
        darkPrimary = Color(0xFF69E6B1),
        darkSecondary = Color(0xFF41DDE8),
        darkTertiary = Color(0xFFFFD27A),
    ),
    ThemePalette.PROFESSIONAL_GRAY to PaletteColors(
        primary = Color(0xFF314157),
        secondary = Color(0xFF2C7A7B),
        tertiary = Color(0xFFB85C38),
        darkPrimary = Color(0xFFC8D4E3),
        darkSecondary = Color(0xFF8FE3E0),
        darkTertiary = Color(0xFFFFB08A),
    ),
    ThemePalette.TECH_PURPLE to PaletteColors(
        primary = Color(0xFF6A3DE8),
        secondary = Color(0xFF00A7B7),
        tertiary = Color(0xFFFFB020),
        darkPrimary = Color(0xFFC8B6FF),
        darkSecondary = Color(0xFF41DDE8),
        darkTertiary = Color(0xFFFFD27A),
    ),
)

private val GuardianShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
)

private val GuardianTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private fun colorScheme(
    darkTheme: Boolean,
    palette: ThemePalette,
    highContrast: Boolean,
): ColorScheme {
    val colors = PaletteMap.getValue(palette)
    return if (darkTheme) {
        val baseSurface = if (highContrast) Color(0xFF050505) else Color(0xFF101820)
        val baseVariant = if (highContrast) Color(0xFF1D1D1D) else Color(0xFF1A2530)
        val primary = if (highContrast) Color.White else colors.darkPrimary
        val secondary = if (highContrast) Color(0xFFE8F7FF) else colors.darkSecondary
        val primaryContainer = if (highContrast) {
            Color(0xFFEFEFEF)
        } else {
            lerp(baseSurface, colors.darkPrimary, 0.24f)
        }
        val secondaryContainer = if (highContrast) {
            Color(0xFFD9F7FF)
        } else {
            lerp(baseSurface, colors.darkSecondary, 0.20f)
        }
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF071018),
            secondary = secondary,
            onSecondary = Color(0xFF061518),
            tertiary = colors.darkTertiary,
            onTertiary = Color(0xFF1F1400),
            background = if (highContrast) Color.Black else Color(0xFF070A0F),
            surface = baseSurface,
            surfaceVariant = if (highContrast) baseVariant else lerp(baseVariant, colors.darkPrimary, 0.10f),
            primaryContainer = primaryContainer,
            onPrimaryContainer = if (highContrast) Color.Black else Color(0xFFE8F3FF),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = if (highContrast) Color.Black else Color(0xFFDAFAFF),
            tertiaryContainer = if (highContrast) Color(0xFFFFF4D8) else lerp(baseSurface, colors.darkTertiary, 0.22f),
            onTertiaryContainer = if (highContrast) Color.Black else Color(0xFFFFE7B6),
            onBackground = Color(0xFFF0F5FA),
            onSurface = Color(0xFFF0F5FA),
            onSurfaceVariant = Color(0xFFC2CED8),
            outline = Color(0xFF6D7C8A),
            outlineVariant = if (highContrast) Color(0xFF2D3A46) else lerp(Color(0xFF2D3A46), colors.darkPrimary, 0.14f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF330001),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = Color(0xFFE2E8EF),
            inverseOnSurface = Color(0xFF111820),
        )
    } else {
        val baseBackground = if (highContrast) Color.White else Color(0xFFF4F8FB)
        val primary = if (highContrast) Color.Black else colors.primary
        val secondary = if (highContrast) Color(0xFF1B1B1B) else colors.secondary
        val surfaceVariant = if (highContrast) {
            Color(0xFFE8F0F6)
        } else {
            lerp(Color.White, colors.primary, 0.07f)
        }
        val primaryContainer = if (highContrast) {
            Color(0xFFEFEFEF)
        } else {
            lerp(Color.White, colors.primary, 0.15f)
        }
        val secondaryContainer = if (highContrast) {
            Color(0xFFE8E8E8)
        } else {
            lerp(Color.White, colors.secondary, 0.16f)
        }
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = colors.tertiary,
            onTertiary = Color(0xFF241400),
            background = baseBackground,
            surface = Color(0xFFFFFFFF),
            surfaceVariant = surfaceVariant,
            primaryContainer = primaryContainer,
            onPrimaryContainer = Color(0xFF061C35),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = Color(0xFF031F24),
            tertiaryContainer = if (highContrast) Color(0xFFF0F0F0) else lerp(Color.White, colors.tertiary, 0.28f),
            onTertiaryContainer = Color(0xFF271700),
            onBackground = Color(0xFF101820),
            onSurface = Color(0xFF101820),
            onSurfaceVariant = Color(0xFF485766),
            outline = Color(0xFF728292),
            outlineVariant = if (highContrast) Color(0xFFD2DDE8) else lerp(Color(0xFFD2DDE8), colors.primary, 0.10f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            inverseSurface = Color(0xFF1C2730),
            inverseOnSurface = Color(0xFFEAF1F7),
        )
    }
}

@Composable
fun CallGuardianTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: ThemePalette = ThemePalette.SECURITY_BLUE,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = colorScheme(darkTheme, palette, highContrast),
        typography = GuardianTypography,
        shapes = GuardianShapes,
        content = content,
    )
}

fun ColorScheme.statusColor(ok: Boolean): Color = if (ok) secondary else error
