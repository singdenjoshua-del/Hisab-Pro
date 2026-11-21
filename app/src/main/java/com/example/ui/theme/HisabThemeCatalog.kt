package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class HisabThemePreset(
    val id: String,
    val nameEn: String,
    val nameNe: String,
    val isDarkDefault: Boolean,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val darkBackground: Color,
    val darkSurface: Color,
    val darkSurfaceVariant: Color,
    val lightBackground: Color,
    val lightSurface: Color,
    val lightSurfaceVariant: Color,
    val heroGradientStart: Color,
    val heroGradientEnd: Color,
    val incomeColor: Color = Color(0xFF10B981),
    val expenseColor: Color = Color(0xFFF43F5E)
)

object HisabThemeCatalog {
    val accentSwatches: List<Pair<String, Color>> = listOf(
        "#10B981" to Color(0xFF10B981), // Emerald
        "#38BDF8" to Color(0xFF38BDF8), // Sky Blue
        "#8B5CF6" to Color(0xFF8B5CF6), // Violet
        "#F59E0B" to Color(0xFFF59E0B), // Amber Gold
        "#F43F5E" to Color(0xFFF43F5E), // Rose
        "#EC4899" to Color(0xFFEC4899), // Magenta
        "#06B6D4" to Color(0xFF06B6D4), // Cyan
        "#22C55E" to Color(0xFF22C55E), // Fresh Green
        "#EAB308" to Color(0xFFEAB308), // Champagne
        "#6366F1" to Color(0xFF6366F1)  // Indigo
    )

    val presets: List<HisabThemePreset> = listOf(
        HisabThemePreset(
            id = "midnight_black",
            nameEn = "Midnight Black",
            nameNe = "मिडनाइट कालो",
            isDarkDefault = true,
            primary = Color(0xFF10B981),
            secondary = Color(0xFF38BDF8),
            tertiary = Color(0xFFF59E0B),
            darkBackground = Color(0xFF090D16),
            darkSurface = Color(0xFF111827),
            darkSurfaceVariant = Color(0xFF1E293B),
            lightBackground = Color(0xFFF4F7FB),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFE8EEF6),
            heroGradientStart = Color(0xFF0F172A),
            heroGradientEnd = Color(0xFF064E3B)
        ),
        HisabThemePreset(
            id = "ocean_blue",
            nameEn = "Ocean Blue",
            nameNe = "ओसियन निलो",
            isDarkDefault = true,
            primary = Color(0xFF38BDF8),
            secondary = Color(0xFF0EA5E9),
            tertiary = Color(0xFF2DD4BF),
            darkBackground = Color(0xFF061322),
            darkSurface = Color(0xFF0C223B),
            darkSurfaceVariant = Color(0xFF153252),
            lightBackground = Color(0xFFF0F7FF),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFDFEEFF),
            heroGradientStart = Color(0xFF0C4A6E),
            heroGradientEnd = Color(0xFF0284C7)
        ),
        HisabThemePreset(
            id = "royal_purple",
            nameEn = "Royal Purple",
            nameNe = "रोयल बैजनी",
            isDarkDefault = true,
            primary = Color(0xFFA855F7),
            secondary = Color(0xFFEC4899),
            tertiary = Color(0xFF38BDF8),
            darkBackground = Color(0xFF110A1F),
            darkSurface = Color(0xFF1C1233),
            darkSurfaceVariant = Color(0xFF2B1D4A),
            lightBackground = Color(0xFFF8F5FF),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFEDE5FF),
            heroGradientStart = Color(0xFF4C1D95),
            heroGradientEnd = Color(0xFF9333EA)
        ),
        HisabThemePreset(
            id = "emerald_green",
            nameEn = "Emerald Green",
            nameNe = "पन्ना हरियो",
            isDarkDefault = true,
            primary = Color(0xFF10B981),
            secondary = Color(0xFF34D399),
            tertiary = Color(0xFFFBBF24),
            darkBackground = Color(0xFF041712),
            darkSurface = Color(0xFF0A2920),
            darkSurfaceVariant = Color(0xFF123B2F),
            lightBackground = Color(0xFFECFDF5),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFD1FAE5),
            heroGradientStart = Color(0xFF064E3B),
            heroGradientEnd = Color(0xFF059669)
        ),
        HisabThemePreset(
            id = "sunset_orange",
            nameEn = "Sunset Orange",
            nameNe = "सूर्यास्त सुन्तला",
            isDarkDefault = true,
            primary = Color(0xFFF97316),
            secondary = Color(0xFFFB7185),
            tertiary = Color(0xFFFACC15),
            darkBackground = Color(0xFF1A0E0A),
            darkSurface = Color(0xFF291711),
            darkSurfaceVariant = Color(0xFF3D2219),
            lightBackground = Color(0xFFFFF7ED),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFFFEDD5),
            heroGradientStart = Color(0xFF9A3412),
            heroGradientEnd = Color(0xFFEA580C)
        ),
        HisabThemePreset(
            id = "rose_gold",
            nameEn = "Rose Gold",
            nameNe = "रोज गोल्ड",
            isDarkDefault = false,
            primary = Color(0xFFB76E79),
            secondary = Color(0xFFE0A96D),
            tertiary = Color(0xFF9D4E5D),
            darkBackground = Color(0xFF1B1114),
            darkSurface = Color(0xFF28191E),
            darkSurfaceVariant = Color(0xFF3A252C),
            lightBackground = Color(0xFFFFF5F6),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFFCE7EA),
            heroGradientStart = Color(0xFF881337),
            heroGradientEnd = Color(0xFFD97706)
        ),
        HisabThemePreset(
            id = "minimal_white",
            nameEn = "Minimal White",
            nameNe = "मिनिमल सेतो",
            isDarkDefault = false,
            primary = Color(0xFF0F172A),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFF6366F1),
            darkBackground = Color(0xFF111827),
            darkSurface = Color(0xFF1F2937),
            darkSurfaceVariant = Color(0xFF374151),
            lightBackground = Color(0xFFF9FAFB),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFF1F5F9),
            heroGradientStart = Color(0xFF0F172A),
            heroGradientEnd = Color(0xFF334155)
        ),
        HisabThemePreset(
            id = "amoled_dark",
            nameEn = "AMOLED Dark",
            nameNe = "एमोलेड कालो",
            isDarkDefault = true,
            primary = Color(0xFF22D3EE),
            secondary = Color(0xFF10B981),
            tertiary = Color(0xFFF59E0B),
            darkBackground = Color(0xFF000000),
            darkSurface = Color(0xFF0B0B0F),
            darkSurfaceVariant = Color(0xFF16161D),
            lightBackground = Color(0xFFF8FAFC),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFE2E8F0),
            heroGradientStart = Color(0xFF000000),
            heroGradientEnd = Color(0xFF0F172A)
        ),
        HisabThemePreset(
            id = "cyber_neon",
            nameEn = "Cyber Neon",
            nameNe = "साइबर नियोन",
            isDarkDefault = true,
            primary = Color(0xFF00F5D4),
            secondary = Color(0xFFF72585),
            tertiary = Color(0xFF7209B7),
            darkBackground = Color(0xFF070614),
            darkSurface = Color(0xFF110E26),
            darkSurfaceVariant = Color(0xFF1D183D),
            lightBackground = Color(0xFFF4F3FF),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFE5E1FF),
            heroGradientStart = Color(0xFF3A0CA3),
            heroGradientEnd = Color(0xFFF72585)
        ),
        HisabThemePreset(
            id = "aurora",
            nameEn = "Aurora",
            nameNe = "अरोरा इन्द्रेणी",
            isDarkDefault = true,
            primary = Color(0xFF2DD4BF),
            secondary = Color(0xFF818CF8),
            tertiary = Color(0xFFF472B6),
            darkBackground = Color(0xFF09121E),
            darkSurface = Color(0xFF101F33),
            darkSurfaceVariant = Color(0xFF1A314D),
            lightBackground = Color(0xFFF0FDFA),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFCCFBF1),
            heroGradientStart = Color(0xFF0F766E),
            heroGradientEnd = Color(0xFF4F46E5)
        ),
        HisabThemePreset(
            id = "champagne_gold",
            nameEn = "Champagne Gold",
            nameNe = "श्याम्पेन सुनौलो",
            isDarkDefault = true,
            primary = Color(0xFFD4AF37),
            secondary = Color(0xFFF3E5AB),
            tertiary = Color(0xFF10B981),
            darkBackground = Color(0xFF12100B),
            darkSurface = Color(0xFF1E1A12),
            darkSurfaceVariant = Color(0xFF2D271B),
            lightBackground = Color(0xFFFEFCE8),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFFEF3C7),
            heroGradientStart = Color(0xFF451A03),
            heroGradientEnd = Color(0xFFB45309)
        ),
        HisabThemePreset(
            id = "forest_green",
            nameEn = "Forest Green",
            nameNe = "वन हरियो",
            isDarkDefault = true,
            primary = Color(0xFF4ADE80),
            secondary = Color(0xFF84CC16),
            tertiary = Color(0xFF38BDF8),
            darkBackground = Color(0xFF07130B),
            darkSurface = Color(0xFF0F2417),
            darkSurfaceVariant = Color(0xFF183825),
            lightBackground = Color(0xFFF0FDF4),
            lightSurface = Color(0xFFFFFFFF),
            lightSurfaceVariant = Color(0xFFDCFCE7),
            heroGradientStart = Color(0xFF14532D),
            heroGradientEnd = Color(0xFF15803D)
        )
    )

    fun findById(id: String): HisabThemePreset =
        presets.firstOrNull { it.id == id } ?: presets.first()

    fun parseColorOrFallback(hex: String, fallback: Color): Color =
        try {
            val clean = hex.removePrefix("#")
            val parsed = when (clean.length) {
                6 -> ("FF$clean").toLong(16)
                8 -> clean.toLong(16)
                else -> return fallback
            }
            Color(parsed)
        } catch (_: Exception) {
            fallback
        }
}
