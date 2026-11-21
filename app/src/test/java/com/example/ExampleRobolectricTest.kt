package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AccountRecord
import com.example.model.CategoryRecord
import com.example.model.CurrencyCode
import com.example.model.CustomerRecord
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.UserSettingsRecord
import com.example.util.ExportHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Hisab Pro", appName)
    }

    @Test
    fun `json backup and csv report round trip preserves financial records`() {
        val accs = listOf(AccountRecord(id = "acc_1", userId = "u1", name = "Cash", accountType = "CASH", balance = 4200.0))
        val cats = listOf(CategoryRecord(id = "cat_1", userId = "u1", nameEn = "Food", nameNe = "खाना", type = "EXPENSE"))
        val custs = listOf(CustomerRecord(id = "cust_1", userId = "u1", name = "Sita", netBalance = 1500.0, totalGiven = 1500.0))
        val txs = listOf(TransactionRecord(id = "tx_1", userId = "u1", title = "Dinner", amount = 500.0, type = "EXPENSE", accountId = "acc_1", accountName = "Cash", transactionEpochMs = 1700000000000L))
        val settings = UserSettingsRecord(id = "preferences", userId = "u1", languageCode = "ne", dateSystem = "BS", currencyCode = "NPR", themeId = "royal_purple")

        val json = ExportHelper.buildJsonBackup(accs, cats, custs, txs, settings)
        val parsed = ExportHelper.parseJsonBackup(json, "u2")

        assertEquals(1, parsed.accounts.size)
        assertEquals(4200.0, parsed.accounts.first().balance, 0.001)
        assertEquals(1, parsed.customers.size)
        assertEquals("Sita", parsed.customers.first().name)
        assertEquals(1, parsed.transactions.size)
        assertEquals(500.0, parsed.transactions.first().amount, 0.001)
        assertEquals("royal_purple", parsed.settings?.themeId)

        val csv = ExportHelper.buildCsvReport(txs, custs, accs, CurrencyCode.NPR, DateSystem.BS, LanguageCode.ENGLISH)
        assertTrue(csv.contains("Dinner"))
        assertTrue(csv.contains("Sita"))
    }
}
