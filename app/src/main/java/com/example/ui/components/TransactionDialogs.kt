package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.AppStrings
import com.example.model.AccountRecord
import com.example.model.CategoryRecord
import com.example.model.CurrencyCode
import com.example.model.CustomerRecord
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.ui.theme.HisabThemeCatalog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabColors
import com.example.util.BikramSambatCalendar
import com.example.util.MoneyFormatter

@Composable
fun TransactionItemCard(
    tx: TransactionRecord,
    categories: List<CategoryRecord>,
    currency: CurrencyCode,
    dateSystem: DateSystem,
    language: LanguageCode,
    devanagariDigits: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val txType = TransactionType.fromWire(tx.type)
    val isPositive = txType == TransactionType.INCOME || txType == TransactionType.CUSTOMER_RECEIVED
    val amountColor = if (isPositive) extra.incomeColor else extra.expenseColor
    val matchedCat = categories.firstOrNull { it.id == tx.categoryId }
    val catColor = HisabThemeCatalog.parseColorOrFallback(
        matchedCat?.colorHex ?: "",
        amountColor
    )
    val catIcon = resolveCategoryIcon(matchedCat?.iconKey ?: "category")
    val catLabel = if (language == LanguageCode.NEPALI && tx.categoryNameNe.isNotBlank()) {
        tx.categoryNameNe
    } else {
        tx.categoryNameEn
    }
    val dateText = BikramSambatCalendar.formatDisplayDate(
        tx.transactionEpochMs,
        dateSystem,
        language,
        devanagariDigits
    )

    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${tx.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = 5.dp,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.16f))
                    .border(1.dp, catColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = catLabel,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        catLabel.takeIf { it.isNotBlank() },
                        tx.accountName.takeIf { it.isNotBlank() },
                        tx.customerName.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = MoneyFormatter.format(
                    amount = if (isPositive) tx.amount else -tx.amount,
                    currency = currency,
                    devanagariDigits = devanagariDigits,
                    showSign = true
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = amountColor
            )
        }
    }
}

@Composable
fun TransactionEditorDialog(
    initialTx: TransactionRecord?,
    defaultType: TransactionType = TransactionType.EXPENSE,
    defaultCustomerId: String = "",
    accounts: List<AccountRecord>,
    categories: List<CategoryRecord>,
    customers: List<CustomerRecord>,
    dateSystem: DateSystem,
    language: LanguageCode,
    devanagariDigits: Boolean,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (TransactionRecord) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val extra = LocalHisabColors.current
    var selectedType by remember {
        mutableStateOf(
            initialTx?.let { TransactionType.fromWire(it.type) } ?: defaultType
        )
    }
    var title by remember { mutableStateOf(initialTx?.title ?: "") }
    var amountText by remember {
        mutableStateOf(initialTx?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var note by remember { mutableStateOf(initialTx?.note ?: "") }
    var selectedEpochMs by remember {
        mutableLongStateOf(initialTx?.transactionEpochMs ?: System.currentTimeMillis())
    }
    var selectedAccountId by remember {
        mutableStateOf(initialTx?.accountId?.ifBlank { accounts.firstOrNull()?.id ?: "" } ?: (accounts.firstOrNull()?.id ?: ""))
    }
    var selectedCategoryId by remember {
        mutableStateOf(initialTx?.categoryId ?: "")
    }
    var selectedCustomerId by remember {
        mutableStateOf(initialTx?.customerId?.ifBlank { defaultCustomerId } ?: defaultCustomerId)
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    val filteredCategories = remember(selectedType, categories) {
        val targetCatType = if (selectedType == TransactionType.INCOME || selectedType == TransactionType.CUSTOMER_RECEIVED) {
            "INCOME"
        } else {
            "EXPENSE"
        }
        categories.filter { it.type == targetCatType }.ifEmpty { categories }
    }

    val effectiveCategory = filteredCategories.firstOrNull { it.id == selectedCategoryId }
        ?: filteredCategories.firstOrNull()
    val effectiveAccount = accounts.firstOrNull { it.id == selectedAccountId }
        ?: accounts.firstOrNull()
    val effectiveCustomer = customers.firstOrNull { it.id == selectedCustomerId }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(26.dp),
            elevation = 16.dp,
            contentPadding = PaddingValues(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTx == null) strings.newTransaction else strings.editTransaction,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (initialTx != null && onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.testTag("delete_transaction_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Transaction Type chips
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        TransactionType.EXPENSE to strings.expense,
                        TransactionType.INCOME to strings.income,
                        TransactionType.CUSTOMER_GAVE to strings.customerGave,
                        TransactionType.CUSTOMER_RECEIVED to strings.customerReceived
                    )
                    types.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = extra.accentColor.copy(alpha = 0.22f),
                                selectedLabelColor = extra.accentColor
                            ),
                            modifier = Modifier.testTag("tx_type_${type.wire}")
                        )
                    }
                }

                GlassTextField(
                    value = title,
                    onValueChange = { title = it; errorText = null },
                    label = { Text(strings.titleLabel) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                GlassTextField(
                    value = amountText,
                    onValueChange = { raw ->
                        if (raw.isEmpty() || raw.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = raw
                            errorText = null
                        }
                    },
                    label = { Text(strings.amountLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                // Date stepper showing live BS / AD date
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        1.dp,
                        if (extra.isDark) Color.White.copy(alpha = 0.14f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.CalendarMonth,
                                contentDescription = strings.dateLabel,
                                tint = extra.accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = strings.dateLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BikramSambatCalendar.formatDisplayDate(
                                        selectedEpochMs,
                                        dateSystem,
                                        language,
                                        devanagariDigits
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Row {
                            IconButton(onClick = { selectedEpochMs -= 86_400_000L }) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous day")
                            }
                            IconButton(onClick = { selectedEpochMs += 86_400_000L }) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next day")
                            }
                        }
                    }
                }

                // Account picker
                Text(
                    text = strings.accountLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEach { acc ->
                        FilterChip(
                            selected = effectiveAccount?.id == acc.id,
                            onClick = { selectedAccountId = acc.id },
                            label = { Text(acc.name) }
                        )
                    }
                }

                // Category picker
                Text(
                    text = strings.categoryLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCategories.forEach { cat ->
                        val catLabel = if (language == LanguageCode.NEPALI) cat.nameNe else cat.nameEn
                        FilterChip(
                            selected = effectiveCategory?.id == cat.id,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(catLabel) }
                        )
                    }
                }

                // Customer picker (especially relevant for Udharo)
                if (customers.isNotEmpty()) {
                    Text(
                        text = strings.customerOptionalLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCustomerId.isBlank(),
                            onClick = { selectedCustomerId = "" },
                            label = { Text("—") }
                        )
                        customers.forEach { cust ->
                            FilterChip(
                                selected = selectedCustomerId == cust.id,
                                onClick = { selectedCustomerId = cust.id },
                                label = { Text(cust.name) }
                            )
                        }
                    }
                }

                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorText != null) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(strings.cancel)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassButton(
                        onClick = {
                            val cleanTitle = title.trim()
                            val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
                            if (cleanTitle.isEmpty()) {
                                errorText = strings.emptyTitleError
                                return@GlassButton
                            }
                            if (parsedAmount <= 0.0) {
                                errorText = strings.invalidAmountError
                                return@GlassButton
                            }
                            val acc = effectiveAccount ?: return@GlassButton
                            onSave(
                                TransactionRecord(
                                    id = initialTx?.id ?: "",
                                    userId = initialTx?.userId ?: "",
                                    title = cleanTitle,
                                    amount = parsedAmount,
                                    type = selectedType.wire,
                                    categoryId = effectiveCategory?.id ?: "",
                                    categoryNameEn = effectiveCategory?.nameEn ?: "Other",
                                    categoryNameNe = effectiveCategory?.nameNe ?: "अन्य",
                                    accountId = acc.id,
                                    accountName = acc.name,
                                    customerId = effectiveCustomer?.id ?: "",
                                    customerName = effectiveCustomer?.name ?: "",
                                    note = note.trim(),
                                    transactionEpochMs = selectedEpochMs
                                )
                            )
                        },
                        accentColor = extra.accentColor,
                        modifier = Modifier.testTag("save_transaction_btn")
                    ) {
                        Text(strings.save, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageOnboardingDialog(
    strings: AppStrings,
    currentLanguage: LanguageCode,
    onSelectLanguage: (LanguageCode) -> Unit,
    onConfirm: () -> Unit
) {
    val extra = LocalHisabColors.current
    AlertDialog(
        onDismissRequest = onConfirm,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = null,
                tint = extra.accentColor,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(strings.chooseLanguageTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings.chooseLanguageSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LanguageCode.entries.forEach { lang ->
                    val selected = lang == currentLanguage
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) extra.accentColor else MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onSelectLanguage(lang) }
                            .testTag("onboarding_lang_${lang.code}"),
                        color = if (selected) extra.accentColor.copy(alpha = 0.14f) else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = extra.accentColor
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = extra.accentColor),
                modifier = Modifier.testTag("onboarding_lang_continue")
            ) {
                Text(strings.continueBtn)
            }
        }
    )
}
