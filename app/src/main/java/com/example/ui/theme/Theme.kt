package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.model.AppearanceMode
import com.example.model.BackgroundStyle
import com.example.model.LanguageCode

data class HisabExtraColors(
    val preset: HisabThemePreset,
    val accentColor: Color,
    val incomeColor: Color,
    val expenseColor: Color,
    val backgroundStyle: BackgroundStyle,
    val isDark: Boolean
)

val LocalHisabColors = staticCompositionLocalOf {
    HisabExtraColors(
        preset = HisabThemeCatalog.presets.first(),
        accentColor = Color(0xFF10B981),
        incomeColor = Color(0xFF10B981),
        expenseColor = Color(0xFFF43F5E),
        backgroundStyle = BackgroundStyle.GRADIENT,
        isDark = true
    )
}

private val HisabShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HisabProTheme(
    themeId: String = "midnight_black",
    accentColorHex: String = "#10B981",
    appearanceMode: AppearanceMode = AppearanceMode.DARK,
    backgroundStyle: BackgroundStyle = BackgroundStyle.GRADIENT,
    language: LanguageCode = LanguageCode.ENGLISH,
    content: @Composable () -> Unit
) {
    val preset = HisabThemeCatalog.findById(themeId)
    val systemDark = isSystemInDarkTheme()
    val useDark = when (appearanceMode) {
        AppearanceMode.SYSTEM -> if (preset.id == "minimal_white" || preset.id == "rose_gold") systemDark else preset.isDarkDefault || systemDark
        AppearanceMode.DARK -> true
        AppearanceMode.LIGHT -> false
    }

    val customAccent = HisabThemeCatalog.parseColorOrFallback(accentColorHex, preset.primary)
    val onAccent = if (customAccent.luminance() > 0.45f) Color(0xFF0A0E14) else Color.White

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = customAccent,
            onPrimary = onAccent,
            primaryContainer = preset.darkSurfaceVariant,
            onPrimaryContainer = Color.White,
            secondary = preset.secondary,
            onSecondary = Color(0xFF090D16),
            secondaryContainer = preset.darkSurfaceVariant,
            onSecondaryContainer = Color(0xFFE2E8F0),
            tertiary = preset.tertiary,
            background = preset.darkBackground,
            onBackground = Color(0xFFF8FAFC),
            surface = preset.darkSurface,
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = preset.darkSurfaceVariant,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF334155),
            error = Color(0xFFF43F5E),
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = customAccent,
            onPrimary = onAccent,
            primaryContainer = preset.lightSurfaceVariant,
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = preset.secondary,
            onSecondary = Color.White,
            secondaryContainer = preset.lightSurfaceVariant,
            onSecondaryContainer = Color(0xFF1E293B),
            tertiary = preset.tertiary,
            background = preset.lightBackground,
            onBackground = Color(0xFF0F172A),
            surface = preset.lightSurface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = preset.lightSurfaceVariant,
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = Color(0xFFE11D48),
            onError = Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !useDark
                controller.isAppearanceLightNavigationBars = !useDark
            }
        }
    }

    val extra = HisabExtraColors(
        preset = preset,
        accentColor = customAccent,
        incomeColor = preset.incomeColor,
        expenseColor = preset.expenseColor,
        backgroundStyle = backgroundStyle,
        isDark = useDark
    )

    CompositionLocalProvider(LocalHisabColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = hisabTypography(language),
            shapes = HisabShapes,
            content = content
        )
    }
}
