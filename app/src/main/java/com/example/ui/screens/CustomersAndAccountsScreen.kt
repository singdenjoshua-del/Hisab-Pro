package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.i18n.AppStrings
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.CurrencyCode
import com.example.model.CustomerRecord
import com.example.model.TransactionType
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.components.resolveAccountIcon
import com.example.ui.theme.HisabThemeCatalog
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabDashboardData
import com.example.util.MoneyFormatter

@Composable
fun CustomersAndAccountsScreen(
    data: HisabDashboardData,
    strings: AppStrings,
    onSaveCustomer: (CustomerRecord) -> Unit,
    onDeleteCustomer: (String) -> Unit,
    onQuickCustomerTx: (CustomerRecord, TransactionType) -> Unit,
    onSaveAccount: (AccountRecord) -> Unit,
    onDeleteAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val currency = CurrencyCode.fromCode(data.settings.currencyCode)
    val devanagariDigits = data.settings.devanagariDigits

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingCustomer by remember { mutableStateOf<CustomerRecord?>(null) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountRecord?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ledger_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("tab_customers")
                    ) {
                        Text(strings.customersTab)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("tab_accounts")
                    ) {
                        Text(strings.accountsTab)
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Customers Receivable/Payable Summary
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        tintColor = extra.incomeColor.copy(alpha = 0.26f),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = strings.receivableLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = extra.incomeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyFormatter.format(data.totalReceivable, currency, devanagariDigits),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = extra.incomeColor
                        )
                    }
                    GlassCard(
                        modifier = Modifier.weight(1f),
                        tintColor = extra.expenseColor.copy(alpha = 0.26f),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(
                            text = strings.payableLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = extra.expenseColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = MoneyFormatter.format(data.totalPayable, currency, devanagariDigits),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = extra.expenseColor
                        )
                    }
                }
            }

            item {
                GlassButton(
                    onClick = {
                        editingCustomer = null
                        showCustomerDialog = true
                    },
                    accentColor = extra.accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .testTag("add_customer_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.addCustomer, fontWeight = FontWeight.SemiBold)
                }
            }

            if (data.customers.isEmpty()) {
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
                                imageVector = Icons.Rounded.People,
                                contentDescription = null,
                                tint = extra.accentColor,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(strings.noCustomersTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                strings.noCustomersHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(data.customers, key = { it.id }) { customer ->
                    val custColor = HisabThemeCatalog.parseColorOrFallback(customer.colorHex, extra.accentColor)
                    val statusColor = when {
                        customer.netBalance > 0 -> extra.incomeColor
                        customer.netBalance < 0 -> extra.expenseColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val statusText = when {
                        customer.netBalance > 0 -> strings.receivableLabel
                        customer.netBalance < 0 -> strings.payableLabel
                        else -> strings.settledLabel
                    }

                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp),
                        shape = RoundedCornerShape(20.dp),
                        onClick = {
                            editingCustomer = customer
                            showCustomerDialog = true
                        },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(custColor.copy(alpha = 0.18f))
                                        .border(1.dp, custColor.copy(alpha = 0.35f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = customer.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = custColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (customer.phone.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Rounded.Phone,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = customer.phone,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor
                                    )
                                    Text(
                                        text = MoneyFormatter.format(
                                            kotlin.math.abs(customer.netBalance),
                                            currency,
                                            devanagariDigits
                                        ),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = statusColor
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassOutlinedButton(
                                    onClick = { onQuickCustomerTx(customer, TransactionType.CUSTOMER_GAVE) },
                                    tintColor = extra.expenseColor,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.gaveCreditBtn, fontWeight = FontWeight.SemiBold)
                                }
                                GlassOutlinedButton(
                                    onClick = { onQuickCustomerTx(customer, TransactionType.CUSTOMER_RECEIVED) },
                                    tintColor = extra.incomeColor,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.receivedPaymentBtn, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Accounts tab
            item {
                GlassButton(
                    onClick = {
                        editingAccount = null
                        showAccountDialog = true
                    },
                    accentColor = extra.accentColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                        .testTag("add_account_btn")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.addAccount, fontWeight = FontWeight.SemiBold)
                }
            }

            items(data.accounts, key = { it.id }) { account ->
                val accColor = HisabThemeCatalog.parseColorOrFallback(account.colorHex, extra.accentColor)
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    onClick = {
                        editingAccount = account
                        showAccountDialog = true
                    },
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(accColor.copy(alpha = 0.16f))
                                .border(1.dp, accColor.copy(alpha = 0.30f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveAccountIcon(account.accountType),
                                contentDescription = account.name,
                                tint = accColor
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = account.accountType,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = MoneyFormatter.format(account.balance, currency, devanagariDigits),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (account.balance >= 0) MaterialTheme.colorScheme.onSurface else extra.expenseColor
                        )
                    }
                }
            }
        }
    }

    if (showCustomerDialog) {
        CustomerEditorDialog(
            initial = editingCustomer,
            strings = strings,
            onDismiss = { showCustomerDialog = false },
            onSave = { cust ->
                onSaveCustomer(cust)
                showCustomerDialog = false
            },
            onDelete = editingCustomer?.let { c ->
                {
                    onDeleteCustomer(c.id)
                    showCustomerDialog = false
                }
            }
        )
    }

    if (showAccountDialog) {
        AccountEditorDialog(
            initial = editingAccount,
            strings = strings,
            onDismiss = { showAccountDialog = false },
            onSave = { acc ->
                onSaveAccount(acc)
                showAccountDialog = false
            },
            onDelete = editingAccount?.let { a ->
                {
                    onDeleteAccount(a.id)
                    showAccountDialog = false
                }
            }
        )
    }
}

@Composable
private fun CustomerEditorDialog(
    initial: CustomerRecord?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (CustomerRecord) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.addCustomer, fontWeight = FontWeight.Bold)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.customerName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phoneOptional) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(strings.noteLabel) },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: CustomerRecord()).copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                note = note.trim()
                            )
                        )
                    }
                }
            ) {
                Text(strings.save, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun AccountEditorDialog(
    initial: AccountRecord?,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (AccountRecord) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var balanceText by remember {
        mutableStateOf(initial?.balance?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "0")
    }
    var accountType by remember {
        mutableStateOf(initial?.let { AccountType.fromWire(it.accountType) } ?: AccountType.CASH)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(strings.addAccount, fontWeight = FontWeight.Bold)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.accountName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text(strings.initialBalance) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.accountTypeLabel, style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AccountType.entries.forEach { t ->
                        FilterChip(
                            selected = accountType == t,
                            onClick = { accountType = t },
                            label = { Text(t.wire) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: AccountRecord()).copy(
                                name = name.trim(),
                                accountType = accountType.wire,
                                balance = balanceText.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                }
            ) {
                Text(strings.save, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
