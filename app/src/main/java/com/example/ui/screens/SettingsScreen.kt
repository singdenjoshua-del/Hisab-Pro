package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.i18n.AppStrings
import com.example.model.CurrencyCode
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.UserSettingsRecord
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabDashboardData
import com.example.util.BackupPayload
import com.example.util.ExportHelper
import com.example.util.MoneyFormatter
import com.google.firebase.auth.FirebaseUser

@Composable
fun SettingsScreen(
    user: FirebaseUser?,
    userId: String,
    data: HisabDashboardData,
    strings: AppStrings,
    onUpdateSettings: ((UserSettingsRecord) -> UserSettingsRecord) -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit,
    onRestoreBackup: (BackupPayload) -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extra = LocalHisabColors.current
    val settings = data.settings
    val language = LanguageCode.fromCode(settings.languageCode)
    val dateSystem = DateSystem.fromWire(settings.dateSystem)
    val currency = CurrencyCode.fromCode(settings.currencyCode)

    var showBudgetDialog by remember { mutableStateOf(false) }

    val csvExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csv = ExportHelper.buildCsvReport(
                    transactions = data.transactions,
                    customers = data.customers,
                    accounts = data.accounts,
                    currency = currency,
                    dateSystem = dateSystem,
                    language = language
                )
                ExportHelper.writeToUri(context, uri, csv)
                onMessage(strings.exportSuccess)
            } catch (e: Exception) {
                onMessage(e.localizedMessage ?: "Export failed")
            }
        }
    }

    val jsonExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = ExportHelper.buildJsonBackup(
                    accounts = data.accounts,
                    categories = data.categories,
                    customers = data.customers,
                    transactions = data.transactions,
                    settings = settings
                )
                ExportHelper.writeToUri(context, uri, json)
                onMessage(strings.exportSuccess)
            } catch (e: Exception) {
                onMessage(e.localizedMessage ?: "Backup failed")
            }
        }
    }

    val jsonImporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val raw = ExportHelper.readFromUri(context, uri)
                val payload = ExportHelper.parseJsonBackup(raw, userId)
                onRestoreBackup(payload)
            } catch (e: Exception) {
                onMessage(e.localizedMessage ?: "Invalid backup file")
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = strings.settingsTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            )
        }

        // Google Profile & Cloud Sync Glass Card
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .testTag("google_profile_card"),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (user?.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = user.displayName ?: strings.googleAccount,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, extra.accentColor.copy(alpha = 0.45f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(extra.accentColor.copy(alpha = 0.16f))
                                    .border(1.5.dp, extra.accentColor.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = strings.googleAccount,
                                    tint = extra.accentColor,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.displayName ?: strings.googleAccount,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = extra.incomeColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = strings.syncedStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = extra.incomeColor
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            onClick = onSwitchAccount,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("switch_account_btn")
                        ) {
                            Icon(Icons.Rounded.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.switchAccount)
                        }
                        GlassOutlinedButton(
                            onClick = onSignOut,
                            tintColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("sign_out_btn")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.signOut)
                        }
                    }
                }
            }
        }

        // Security Settings Link
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .testTag("open_security_settings_btn"),
                shape = RoundedCornerShape(20.dp),
                onClick = onOpenSecuritySettings,
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(extra.accentColor.copy(alpha = 0.16f))
                            .border(1.dp, extra.accentColor.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = strings.appLockTitle,
                            tint = extra.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.appLockTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(strings.appLockSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
                }
            }
        }

        // Preferences: Language, Calendar (BS/AD), Devanagari Digits, Currency, Budget
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(strings.preferencesSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Text(strings.languageLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LanguageCode.entries.forEach { lang ->
                            FilterChip(
                                selected = language == lang,
                                onClick = {
                                    onUpdateSettings {
                                        it.copy(
                                            languageCode = lang.code,
                                            devanagariDigits = lang == LanguageCode.NEPALI,
                                            languageSelected = true
                                        )
                                    }
                                },
                                label = { Text(lang.displayName) },
                                modifier = Modifier.testTag("settings_lang_${lang.code}")
                            )
                        }
                    }

                    Text(strings.dateSystemLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            DateSystem.BS to "BS (वि.सं.)",
                            DateSystem.AD to "AD (Gregorian)",
                            DateSystem.BOTH to "BS + AD"
                        ).forEach { (ds, label) ->
                            FilterChip(
                                selected = dateSystem == ds,
                                onClick = { onUpdateSettings { it.copy(dateSystem = ds.wire) } },
                                label = { Text(label) },
                                modifier = Modifier.testTag("settings_date_${ds.wire}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.devanagariNumerals, style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = settings.devanagariDigits,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(devanagariDigits = checked) }
                            }
                        )
                    }

                    Text(strings.currencyLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurrencyCode.entries.forEach { curr ->
                            FilterChip(
                                selected = currency == curr,
                                onClick = { onUpdateSettings { it.copy(currencyCode = curr.code) } },
                                label = { Text("${curr.code} (${curr.symbol})") },
                                modifier = Modifier.testTag("settings_currency_${curr.code}")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showBudgetDialog = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.monthlyBudgetLabel, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = MoneyFormatter.format(settings.monthlyBudget, currency, settings.devanagariDigits),
                            style = MaterialTheme.typography.titleMedium,
                            color = extra.accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Export, Download & Restore Backup
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.exportBackupSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    GlassButton(
                        onClick = { csvExporter.launch("hisab_pro_report.csv") },
                        accentColor = extra.accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportCsvBtn, fontWeight = FontWeight.SemiBold)
                    }

                    GlassOutlinedButton(
                        onClick = { jsonExporter.launch("hisab_pro_backup.json") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_json_btn")
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.exportJsonBtn)
                    }

                    GlassOutlinedButton(
                        onClick = {
                            val csv = ExportHelper.buildCsvReport(
                                transactions = data.transactions,
                                customers = data.customers,
                                accounts = data.accounts,
                                currency = currency,
                                dateSystem = dateSystem,
                                language = language
                            )
                            val intent = ExportHelper.createShareIntent(
                                context = context,
                                filename = "hisab_pro_report.csv",
                                mimeType = "text/csv",
                                content = csv
                            )
                            context.startActivity(android.content.Intent.createChooser(intent, strings.shareReportBtn))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_report_btn")
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.shareReportBtn)
                    }

                    GlassOutlinedButton(
                        onClick = { jsonImporter.launch(arrayOf("application/json", "text/plain")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_json_btn")
                    ) {
                        Icon(Icons.Rounded.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.restoreJsonBtn)
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        var budgetText by remember {
            mutableStateOf(
                if (settings.monthlyBudget % 1.0 == 0.0) settings.monthlyBudget.toLong().toString()
                else settings.monthlyBudget.toString()
            )
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(strings.monthlyBudgetLabel, fontWeight = FontWeight.Bold) },
            text = {
                GlassTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                GlassButton(
                    onClick = {
                        val valDouble = budgetText.toDoubleOrNull()
                        if (valDouble != null && valDouble >= 0.0) {
                            onUpdateSettings { it.copy(monthlyBudget = valDouble) }
                        }
                        showBudgetDialog = false
                    }
                ) { Text(strings.save, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
