package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.CurrencyCode
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassQuickActionTile
import com.example.ui.components.GlassTagPill
import com.example.ui.components.TransactionItemCard
import com.example.ui.components.WeeklyCashflowChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabDashboardData
import com.example.util.BikramSambatCalendar
import com.example.util.MoneyFormatter

@Composable
fun DashboardScreen(
    data: HisabDashboardData,
    strings: AppStrings,
    onViewAllTransactions: () -> Unit,
    onEditTransaction: (TransactionRecord) -> Unit,
    onQuickAddIncome: () -> Unit = {},
    onQuickAddExpense: () -> Unit = {},
    onOpenLedger: () -> Unit = {},
    onOpenThemes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val currency = CurrencyCode.fromCode(data.settings.currencyCode)
    val dateSystem = DateSystem.fromWire(data.settings.dateSystem)
    val language = LanguageCode.fromCode(data.settings.languageCode)
    val devanagariDigits = data.settings.devanagariDigits
    val todayDateStr = BikramSambatCalendar.formatDisplayDate(
        System.currentTimeMillis(),
        dateSystem,
        language,
        devanagariDigits
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = todayDateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlassTagPill(
                    text = "${currency.code} (${currency.symbol})",
                    accentColor = extra.accentColor
                )
            }
        }

        // Hero Frosted Glass Financial Card
        item {
            val heroShape = RoundedCornerShape(28.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = heroShape,
                        ambientColor = extra.accentColor.copy(alpha = 0.35f),
                        spotColor = Color.Black.copy(alpha = 0.45f)
                    )
                    .clip(heroShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                extra.preset.heroGradientStart.copy(alpha = 0.90f),
                                extra.preset.heroGradientEnd.copy(alpha = 0.84f)
                            )
                        )
                    )
                    // Translucent top specular glass sheen overlay
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.02f),
                                Color.Black.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.42f),
                                Color.White.copy(alpha = 0.12f),
                                extra.accentColor.copy(alpha = 0.35f)
                            )
                        ),
                        shape = heroShape
                    )
                    .padding(22.dp)
                    .testTag("hero_balance_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.totalBalance,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.84f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyFormatter.format(
                                    data.totalBalance,
                                    currency,
                                    devanagariDigits
                                ),
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(
                                    1.dp,
                                    Color.White.copy(alpha = 0.30f),
                                    RoundedCornerShape(15.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = strings.totalBalance,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Frosted Available Balance Pill
                    Surface(
                        color = Color.White.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.availableBalance,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                            Text(
                                text = MoneyFormatter.format(
                                    data.availableBalance,
                                    currency,
                                    devanagariDigits
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Frosted Income & Expenses Split
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        HeroMiniMetric(
                            icon = Icons.Rounded.ArrowDownward,
                            label = strings.totalIncome,
                            amountText = MoneyFormatter.format(
                                data.totalIncome,
                                currency,
                                devanagariDigits
                            ),
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.weight(1f)
                        )
                        HeroMiniMetric(
                            icon = Icons.Rounded.ArrowUpward,
                            label = strings.totalExpenses,
                            amountText = MoneyFormatter.format(
                                data.totalExpenses,
                                currency,
                                devanagariDigits
                            ),
                            tint = Color(0xFFFDA4AF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Glass Quick Action Buttons Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassQuickActionTile(
                    icon = Icons.Rounded.AddCircleOutline,
                    label = strings.income,
                    accent = extra.incomeColor,
                    onClick = onQuickAddIncome,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_income")
                )
                GlassQuickActionTile(
                    icon = Icons.Rounded.RemoveCircleOutline,
                    label = strings.expense,
                    accent = extra.expenseColor,
                    onClick = onQuickAddExpense,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_expense")
                )
                GlassQuickActionTile(
                    icon = Icons.Rounded.People,
                    label = strings.navLedger,
                    accent = extra.preset.secondary,
                    onClick = onOpenLedger,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_ledger")
                )
                GlassQuickActionTile(
                    icon = Icons.Rounded.Palette,
                    label = strings.navThemes,
                    accent = extra.accentColor,
                    onClick = onOpenThemes,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_themes")
                )
            }
        }

        // Today's Expenses & Monthly Expenses
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricTileCard(
                    icon = Icons.Rounded.Today,
                    label = strings.todaysExpenses,
                    value = MoneyFormatter.format(data.todaysExpenses, currency, devanagariDigits),
                    accent = extra.expenseColor,
                    modifier = Modifier.weight(1f)
                )
                MetricTileCard(
                    icon = Icons.Rounded.CalendarMonth,
                    label = strings.monthlyExpenses,
                    value = MoneyFormatter.format(data.monthlyExpenses, currency, devanagariDigits),
                    accent = extra.preset.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Customer Khata Summary Glass Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .testTag("dashboard_customer_summary_card"),
                onClick = onOpenLedger,
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(extra.preset.secondary.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        extra.preset.secondary.copy(alpha = 0.30f),
                                        RoundedCornerShape(11.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.People,
                                    contentDescription = strings.customersTab,
                                    tint = extra.preset.secondary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.customersTab,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = strings.navLedger,
                            tint = extra.accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = extra.incomeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, extra.incomeColor.copy(alpha = 0.28f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = strings.receivableLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.incomeColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = MoneyFormatter.format(
                                        data.totalReceivable,
                                        currency,
                                        devanagariDigits
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = extra.incomeColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = extra.expenseColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, extra.expenseColor.copy(alpha = 0.28f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = strings.payableLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extra.expenseColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = MoneyFormatter.format(
                                        data.totalPayable,
                                        currency,
                                        devanagariDigits
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = extra.expenseColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Savings Overview Glass Card
        item {
            val budget = data.settings.monthlyBudget.coerceAtLeast(1.0)
            val budgetRatio = (data.monthlyExpenses / budget).toFloat().coerceIn(0f, 1f)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .testTag("savings_overview_card"),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(extra.accentColor.copy(alpha = 0.16f))
                                    .border(
                                        1.dp,
                                        extra.accentColor.copy(alpha = 0.30f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Savings,
                                    contentDescription = strings.savingsOverview,
                                    tint = extra.accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.savingsOverview,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(
                                data.monthlySavings,
                                currency,
                                devanagariDigits,
                                showSign = true
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (data.monthlySavings >= 0) extra.incomeColor else extra.expenseColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { budgetRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(50)),
                        color = if (budgetRatio > 0.9f) extra.expenseColor else extra.accentColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        strokeCap = StrokeCap.Round
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${strings.monthlyBudget}: ${
                                MoneyFormatter.formatCompact(
                                    data.settings.monthlyBudget,
                                    currency,
                                    devanagariDigits
                                )
                            }",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(budgetRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Interactive 7-Day Cashflow Glass Chart Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.weeklyCashflow,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    WeeklyCashflowChart(
                        bars = data.weeklyCashflow,
                        currency = currency,
                        devanagariDigits = devanagariDigits
                    )
                }
            }
        }

        // Interactive Spending Breakdown Donut Chart Glass Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = strings.spendingByCategory,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    CategoryDonutChart(
                        slices = data.categorySlices,
                        totalExpenses = data.totalExpenses,
                        currency = currency,
                        language = language,
                        devanagariDigits = devanagariDigits,
                        emptyLabel = strings.noTransactionsHint
                    )
                }
            }
        }

        // Recent Transactions Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.recentTransactions,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = onViewAllTransactions,
                    modifier = Modifier.testTag("view_all_transactions_btn")
                ) {
                    Text(strings.viewAll, color = extra.accentColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = extra.accentColor
                    )
                }
            }
        }

        if (data.recentTransactions.isEmpty()) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    contentPadding = PaddingValues(28.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = extra.accentColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = strings.noTransactionsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = strings.noTransactionsHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(data.recentTransactions, key = { it.id }) { tx ->
                Box(modifier = Modifier.widthIn(max = 680.dp)) {
                    TransactionItemCard(
                        tx = tx,
                        categories = data.categories,
                        currency = currency,
                        dateSystem = dateSystem,
                        language = language,
                        devanagariDigits = devanagariDigits,
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMiniMetric(
    icon: ImageVector,
    label: String,
    amountText: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.24f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetricTileCard(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
