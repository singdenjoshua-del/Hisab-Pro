package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.CurrencyCode
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.TransactionItemCard
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabDashboardData

@Composable
fun TransactionsScreen(
    data: HisabDashboardData,
    strings: AppStrings,
    onEditTransaction: (TransactionRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val currency = CurrencyCode.fromCode(data.settings.currencyCode)
    val dateSystem = DateSystem.fromWire(data.settings.dateSystem)
    val language = LanguageCode.fromCode(data.settings.languageCode)
    val devanagariDigits = data.settings.devanagariDigits

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filtered = remember(data.transactions, searchQuery, activeFilter) {
        val q = searchQuery.trim().lowercase()
        data.transactions.filter { tx ->
            val typeOk = activeFilter == null || tx.type == activeFilter?.wire
            val queryOk = q.isEmpty() ||
                tx.title.lowercase().contains(q) ||
                tx.customerName.lowercase().contains(q) ||
                tx.note.lowercase().contains(q) ||
                tx.categoryNameEn.lowercase().contains(q) ||
                tx.categoryNameNe.lowercase().contains(q)
            typeOk && queryOk
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("transactions_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = strings.navTransactions,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                GlassTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(strings.searchTransactions) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transactions_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == null,
                        onClick = { activeFilter = null },
                        label = { Text(strings.all) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = extra.accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = extra.accentColor
                        )
                    )
                    listOf(
                        TransactionType.EXPENSE to strings.expense,
                        TransactionType.INCOME to strings.income,
                        TransactionType.CUSTOMER_GAVE to strings.customerGave,
                        TransactionType.CUSTOMER_RECEIVED to strings.customerReceived
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = activeFilter == type,
                            onClick = { activeFilter = if (activeFilter == type) null else type },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extra.accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = extra.accentColor
                            )
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
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
            items(filtered, key = { it.id }) { tx ->
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
