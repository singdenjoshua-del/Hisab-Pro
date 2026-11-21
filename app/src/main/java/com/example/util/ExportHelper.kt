package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.model.AccountRecord
import com.example.model.CategoryRecord
import com.example.model.CurrencyCode
import com.example.model.CustomerRecord
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.UserSettingsRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class BackupPayload(
    val accounts: List<AccountRecord>,
    val categories: List<CategoryRecord>,
    val customers: List<CustomerRecord>,
    val transactions: List<TransactionRecord>,
    val settings: UserSettingsRecord?
)

object ExportHelper {
    fun buildCsvReport(
        transactions: List<TransactionRecord>,
        customers: List<CustomerRecord>,
        accounts: List<AccountRecord>,
        currency: CurrencyCode,
        dateSystem: DateSystem,
        language: LanguageCode
    ): String {
        val sb = StringBuilder()
        sb.appendLine("HISAB PRO FINANCIAL REPORT")
        sb.appendLine("Currency,${currency.code}")
        sb.appendLine()
        sb.appendLine("ACCOUNTS")
        sb.appendLine("Name,Type,Balance")
        accounts.forEach { acc ->
            sb.appendLine("${csvEscape(acc.name)},${acc.accountType},${acc.balance}")
        }
        sb.appendLine()
        sb.appendLine("CUSTOMERS")
        sb.appendLine("Name,Phone,NetBalance,TotalGiven,TotalReceived,Note")
        customers.forEach { c ->
            sb.appendLine(
                "${csvEscape(c.name)},${csvEscape(c.phone)},${c.netBalance},${c.totalGiven},${c.totalReceived},${csvEscape(c.note)}"
            )
        }
        sb.appendLine()
        sb.appendLine("TRANSACTIONS")
        sb.appendLine("Date,Title,Type,Amount,Category,Account,Customer,Note")
        transactions.sortedByDescending { it.transactionEpochMs }.forEach { tx ->
            val dateStr = BikramSambatCalendar.formatDisplayDate(
                tx.transactionEpochMs,
                dateSystem,
                language,
                false
            )
            val catName = if (language == LanguageCode.NEPALI && tx.categoryNameNe.isNotBlank()) {
                tx.categoryNameNe
            } else {
                tx.categoryNameEn
            }
            sb.appendLine(
                "${csvEscape(dateStr)},${csvEscape(tx.title)},${tx.type},${tx.amount},${csvEscape(catName)},${csvEscape(tx.accountName)},${csvEscape(tx.customerName)},${csvEscape(tx.note)}"
            )
        }
        return sb.toString()
    }

    fun buildJsonBackup(
        accounts: List<AccountRecord>,
        categories: List<CategoryRecord>,
        customers: List<CustomerRecord>,
        transactions: List<TransactionRecord>,
        settings: UserSettingsRecord
    ): String {
        val root = JSONObject()
        root.put("app", "Hisab Pro")
        root.put("version", 1)
        root.put("exportedAtEpochMs", System.currentTimeMillis())

        val accArr = JSONArray()
        accounts.forEach { a ->
            accArr.put(
                JSONObject().apply {
                    put("id", a.id)
                    put("name", a.name)
                    put("accountType", a.accountType)
                    put("balance", a.balance)
                    put("iconKey", a.iconKey)
                    put("colorHex", a.colorHex)
                }
            )
        }
        root.put("accounts", accArr)

        val catArr = JSONArray()
        categories.forEach { c ->
            catArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("nameEn", c.nameEn)
                    put("nameNe", c.nameNe)
                    put("type", c.type)
                    put("iconKey", c.iconKey)
                    put("colorHex", c.colorHex)
                    put("systemDefault", c.systemDefault)
                }
            )
        }
        root.put("categories", catArr)

        val custArr = JSONArray()
        customers.forEach { c ->
            custArr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("phone", c.phone)
                    put("note", c.note)
                    put("netBalance", c.netBalance)
                    put("totalGiven", c.totalGiven)
                    put("totalReceived", c.totalReceived)
                    put("colorHex", c.colorHex)
                }
            )
        }
        root.put("customers", custArr)

        val txArr = JSONArray()
        transactions.forEach { t ->
            txArr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("amount", t.amount)
                    put("type", t.type)
                    put("categoryId", t.categoryId)
                    put("categoryNameEn", t.categoryNameEn)
                    put("categoryNameNe", t.categoryNameNe)
                    put("accountId", t.accountId)
                    put("accountName", t.accountName)
                    put("customerId", t.customerId)
                    put("customerName", t.customerName)
                    put("note", t.note)
                    put("transactionEpochMs", t.transactionEpochMs)
                }
            )
        }
        root.put("transactions", txArr)

        root.put(
            "settings",
            JSONObject().apply {
                put("languageCode", settings.languageCode)
                put("dateSystem", settings.dateSystem)
                put("currencyCode", settings.currencyCode)
                put("themeId", settings.themeId)
                put("accentColorHex", settings.accentColorHex)
                put("appearanceMode", settings.appearanceMode)
                put("backgroundStyle", settings.backgroundStyle)
                put("monthlyBudget", settings.monthlyBudget)
                put("devanagariDigits", settings.devanagariDigits)
                put("languageSelected", settings.languageSelected)
            }
        )
        return root.toString(2)
    }

    fun parseJsonBackup(jsonText: String, uid: String): BackupPayload {
        val root = JSONObject(jsonText)
        val accounts = mutableListOf<AccountRecord>()
        val accArr = root.optJSONArray("accounts")
        if (accArr != null) {
            for (i in 0 until accArr.length()) {
                val o = accArr.getJSONObject(i)
                accounts.add(
                    AccountRecord(
                        id = sanitizeId(o.optString("id"), "acc_$i"),
                        userId = uid,
                        name = o.optString("name", "Account").take(60),
                        accountType = o.optString("accountType", "CASH"),
                        balance = o.optDouble("balance", 0.0),
                        iconKey = o.optString("iconKey", "wallet").take(40),
                        colorHex = o.optString("colorHex", "#10B981").take(16)
                    )
                )
            }
        }

        val categories = mutableListOf<CategoryRecord>()
        val catArr = root.optJSONArray("categories")
        if (catArr != null) {
            for (i in 0 until catArr.length()) {
                val o = catArr.getJSONObject(i)
                categories.add(
                    CategoryRecord(
                        id = sanitizeId(o.optString("id"), "cat_$i"),
                        userId = uid,
                        nameEn = o.optString("nameEn", "Other").take(50),
                        nameNe = o.optString("nameNe", "अन्य").take(80),
                        type = o.optString("type", "EXPENSE"),
                        iconKey = o.optString("iconKey", "category").take(40),
                        colorHex = o.optString("colorHex", "#F59E0B").take(16),
                        systemDefault = o.optBoolean("systemDefault", false)
                    )
                )
            }
        }

        val customers = mutableListOf<CustomerRecord>()
        val custArr = root.optJSONArray("customers")
        if (custArr != null) {
            for (i in 0 until custArr.length()) {
                val o = custArr.getJSONObject(i)
                customers.add(
                    CustomerRecord(
                        id = sanitizeId(o.optString("id"), "cust_$i"),
                        userId = uid,
                        name = o.optString("name", "Customer").take(80),
                        phone = o.optString("phone", "").take(30),
                        note = o.optString("note", "").take(300),
                        netBalance = o.optDouble("netBalance", 0.0),
                        totalGiven = o.optDouble("totalGiven", 0.0).coerceAtLeast(0.0),
                        totalReceived = o.optDouble("totalReceived", 0.0).coerceAtLeast(0.0),
                        colorHex = o.optString("colorHex", "#3B82F6").take(16)
                    )
                )
            }
        }

        val transactions = mutableListOf<TransactionRecord>()
        val txArr = root.optJSONArray("transactions")
        if (txArr != null) {
            for (i in 0 until txArr.length()) {
                val o = txArr.getJSONObject(i)
                val amt = o.optDouble("amount", 0.0)
                if (amt > 0.0) {
                    transactions.add(
                        TransactionRecord(
                            id = sanitizeId(o.optString("id"), "tx_$i"),
                            userId = uid,
                            title = o.optString("title", "Entry").take(120),
                            amount = amt,
                            type = o.optString("type", "EXPENSE"),
                            categoryId = o.optString("categoryId", "").take(128),
                            categoryNameEn = o.optString("categoryNameEn", "").take(60),
                            categoryNameNe = o.optString("categoryNameNe", "").take(80),
                            accountId = o.optString("accountId", "acc_cash").ifBlank { "acc_cash" }.take(128),
                            accountName = o.optString("accountName", "Cash").ifBlank { "Cash" }.take(60),
                            customerId = o.optString("customerId", "").take(128),
                            customerName = o.optString("customerName", "").take(80),
                            note = o.optString("note", "").take(500),
                            transactionEpochMs = o.optLong("transactionEpochMs", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val sObj = root.optJSONObject("settings")
        val settings = sObj?.let {
            UserSettingsRecord(
                id = "preferences",
                userId = uid,
                languageCode = it.optString("languageCode", "en"),
                dateSystem = it.optString("dateSystem", "BS"),
                currencyCode = it.optString("currencyCode", "NPR"),
                themeId = it.optString("themeId", "midnight_black").take(40),
                accentColorHex = it.optString("accentColorHex", "#10B981").take(16),
                appearanceMode = it.optString("appearanceMode", "DARK"),
                backgroundStyle = it.optString("backgroundStyle", "GRADIENT"),
                monthlyBudget = it.optDouble("monthlyBudget", 50000.0).coerceAtLeast(0.0),
                devanagariDigits = it.optBoolean("devanagariDigits", false),
                languageSelected = it.optBoolean("languageSelected", true)
            )
        }

        return BackupPayload(accounts, categories, customers, transactions, settings)
    }

    fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { os ->
            os.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    fun readFromUri(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
    }

    fun createShareIntent(context: Context, filename: String, mimeType: String, content: String): Intent {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, filename)
        file.writeText(content, Charsets.UTF_8)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun sanitizeId(raw: String, fallback: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "")
        return if (cleaned.isNotBlank()) cleaned.take(128) else fallback
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
