package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.AppearanceMode
import com.example.model.BackgroundStyle
import com.example.model.LanguageCode
import com.example.model.UserSettingsRecord
import com.example.ui.components.GlassCard
import com.example.ui.theme.HisabThemeCatalog
import com.example.ui.theme.HisabThemePreset
import com.example.ui.theme.LocalHisabColors

@Composable
fun ThemeStoreScreen(
    settings: UserSettingsRecord,
    strings: AppStrings,
    onUpdateSettings: ((UserSettingsRecord) -> UserSettingsRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val language = LanguageCode.fromCode(settings.languageCode)
    val currentAppearance = AppearanceMode.fromWire(settings.appearanceMode)
    val currentBgStyle = BackgroundStyle.fromWire(settings.backgroundStyle)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("theme_store_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = extra.accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.themeStoreTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = strings.themeStoreSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Appearance Mode (System / Light / Dark)
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = strings.appearanceMode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        val modes = listOf(
                            AppearanceMode.SYSTEM to strings.systemMode,
                            AppearanceMode.LIGHT to strings.lightMode,
                            AppearanceMode.DARK to strings.darkMode
                        )
                        modes.forEachIndexed { idx, (mode, label) ->
                            SegmentedButton(
                                selected = currentAppearance == mode,
                                onClick = {
                                    onUpdateSettings { it.copy(appearanceMode = mode.wire) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size),
                                modifier = Modifier.testTag("appearance_mode_${mode.wire}")
                            ) {
                                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }

                    Text(
                        text = strings.backgroundStyle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            BackgroundStyle.GRADIENT to strings.gradientBg,
                            BackgroundStyle.FLAT to strings.flatBg,
                            BackgroundStyle.AURORA_GLOW to strings.auroraBg
                        ).forEach { (style, label) ->
                            FilterChip(
                                selected = currentBgStyle == style,
                                onClick = { onUpdateSettings { it.copy(backgroundStyle = style.wire) } },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text(
                        text = strings.accentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HisabThemeCatalog.accentSwatches.forEach { (hex, color) ->
                            val isSelected = settings.accentColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onUpdateSettings { it.copy(accentColorHex = hex) }
                                    }
                                    .testTag("accent_swatch_$hex"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 12 Theme Presets (2 per row)
        val rows = HisabThemeCatalog.presets.chunked(2)
        items(rows.size) { rowIndex ->
            val pair = rows[rowIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { preset ->
                    val isActive = settings.themeId == preset.id
                    ThemePreviewCard(
                        preset = preset,
                        language = language,
                        isActive = isActive,
                        strings = strings,
                        onApply = {
                            val hex = String.format(
                                "#%06X",
                                (0xFFFFFF and preset.primary.hashCode())
                            )
                            val matchedHex = HisabThemeCatalog.accentSwatches.firstOrNull {
                                it.second == preset.primary
                            }?.first ?: String.format("#%02X%02X%02X", (preset.primary.red * 255).toInt(), (preset.primary.green * 255).toInt(), (preset.primary.blue * 255).toInt())
                            onUpdateSettings {
                                it.copy(
                                    themeId = preset.id,
                                    accentColorHex = matchedHex,
                                    appearanceMode = if (preset.isDarkDefault) AppearanceMode.DARK.wire else AppearanceMode.LIGHT.wire
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemePreviewCard(
    preset: HisabThemePreset,
    language: LanguageCode,
    isActive: Boolean,
    strings: AppStrings,
    onApply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (preset.isDarkDefault) preset.darkSurface else preset.lightSurface
    val cardInnerBg = if (preset.isDarkDefault) preset.darkBackground else preset.lightBackground
    val titleColor = if (preset.isDarkDefault) Color.White else Color(0xFF0F172A)

    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .shadow(
                elevation = if (isActive) 12.dp else 5.dp,
                shape = cardShape,
                ambientColor = preset.primary.copy(alpha = if (isActive) 0.35f else 0.12f),
                spotColor = Color.Black.copy(alpha = 0.35f)
            )
            .clip(cardShape)
            .border(
                width = if (isActive) 2.2.dp else 1.dp,
                brush = if (isActive) {
                    Brush.linearGradient(listOf(preset.primary, preset.secondary))
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (preset.isDarkDefault) 0.25f else 0.85f),
                            Color.White.copy(alpha = 0.08f)
                        )
                    )
                },
                shape = cardShape
            )
            .clickable(onClick = onApply)
            .testTag("theme_card_${preset.id}"),
        color = bgColor.copy(alpha = 0.86f),
        shape = cardShape,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live mini dashboard preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardInnerBg)
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(preset.heroGradientStart, preset.heroGradientEnd)
                                )
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.85f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(preset.primary)
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preset.primary.copy(alpha = 0.25f))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(preset.secondary.copy(alpha = 0.25f))
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == LanguageCode.NEPALI) preset.nameNe else preset.nameEn,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isActive) {
                    Surface(
                        color = preset.primary,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = strings.activeBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}
