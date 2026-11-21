package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CurrencyCode
import com.example.model.LanguageCode
import com.example.ui.theme.HisabThemeCatalog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.CategorySpendSlice
import com.example.ui.viewmodel.DailyCashflowBar
import com.example.util.MoneyFormatter

@Composable
fun CategoryDonutChart(
    slices: List<CategorySpendSlice>,
    totalExpenses: Double,
    currency: CurrencyCode,
    language: LanguageCode,
    devanagariDigits: Boolean,
    emptyLabel: String,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    var selectedIndex by remember(slices) { mutableIntStateOf(-1) }
    val sweepProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 650),
        label = "donut_sweep"
    )

    if (slices.isEmpty() || totalExpenses <= 0.0) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val selectedSlice = slices.getOrNull(selectedIndex)
    val centerAmount = selectedSlice?.amount ?: totalExpenses
    val centerTitle = selectedSlice?.let {
        if (language == LanguageCode.NEPALI) it.labelNe else it.labelEn
    } ?: if (language == LanguageCode.NEPALI) "कुल खर्च" else "Total"

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .testTag("category_donut_chart"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(136.dp)) {
                val strokeWidth = 22.dp.toPx()
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                var startAngle = -90f

                slices.forEachIndexed { idx, slice ->
                    val sliceColor = HisabThemeCatalog.parseColorOrFallback(
                        slice.colorHex,
                        extra.accentColor
                    )
                    val sweep = (slice.percentage * 360f * sweepProgress).coerceAtLeast(3f)
                    val isSelected = idx == selectedIndex
                    drawArc(
                        color = sliceColor,
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2.5f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 5.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = centerTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = MoneyFormatter.formatCompact(centerAmount, currency, devanagariDigits),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEachIndexed { idx, slice ->
                val sliceColor = HisabThemeCatalog.parseColorOrFallback(slice.colorHex, extra.accentColor)
                val isSelected = idx == selectedIndex
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectedIndex = if (selectedIndex == idx) -1 else idx
                        }
                        .testTag("donut_legend_$idx"),
                    color = if (isSelected) sliceColor.copy(alpha = 0.16f) else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(sliceColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == LanguageCode.NEPALI) slice.labelNe else slice.labelEn,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${(slice.percentage * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyCashflowChart(
    bars: List<DailyCashflowBar>,
    currency: CurrencyCode,
    devanagariDigits: Boolean,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val maxVal = remember(bars) {
        bars.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1.0) ?: 1.0
    }
    var selectedDay by remember { mutableIntStateOf(-1) }
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600),
        label = "bar_anim"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        val activeBar = bars.getOrNull(selectedDay)
        if (activeBar != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activeBar.dayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "+${MoneyFormatter.formatCompact(activeBar.income, currency, devanagariDigits)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = extra.incomeColor
                    )
                    Text(
                        text = "-${MoneyFormatter.formatCompact(activeBar.expense, currency, devanagariDigits)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = extra.expenseColor
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .testTag("weekly_cashflow_chart"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEachIndexed { idx, bar ->
                val isSelected = idx == selectedDay
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedDay = if (selectedDay == idx) -1 else idx }
                        .padding(horizontal = 2.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                    ) {
                        val barWidth = (size.width * 0.28f).coerceAtLeast(6.dp.toPx())
                        val gap = 3.dp.toPx()
                        val centerX = size.width / 2f

                        val incHeight = ((bar.income / maxVal).toFloat() * size.height * animProgress)
                            .coerceAtLeast(if (bar.income > 0) 6.dp.toPx() else 3.dp.toPx())
                        val expHeight = ((bar.expense / maxVal).toFloat() * size.height * animProgress)
                            .coerceAtLeast(if (bar.expense > 0) 6.dp.toPx() else 3.dp.toPx())

                        drawRoundRect(
                            color = if (bar.income > 0) extra.incomeColor else extra.incomeColor.copy(alpha = 0.2f),
                            topLeft = Offset(centerX - barWidth - gap / 2f, size.height - incHeight),
                            size = Size(barWidth, incHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                        drawRoundRect(
                            color = if (bar.expense > 0) extra.expenseColor else extra.expenseColor.copy(alpha = 0.2f),
                            topLeft = Offset(centerX + gap / 2f, size.height - expHeight),
                            size = Size(barWidth, expHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = bar.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) extra.accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
