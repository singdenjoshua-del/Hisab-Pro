package com.example

import com.example.model.CurrencyCode
import com.example.model.LanguageCode
import com.example.util.BikramSambatCalendar
import com.example.util.MoneyFormatter
import com.example.util.SecurityCrypto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HisabCoreUnitTest {

    @Test
    fun bikramSambatReferenceAndFormatting() {
        val cal = Calendar.getInstance().apply {
            set(1943, Calendar.APRIL, 14, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val bs = BikramSambatCalendar.toBs(cal.timeInMillis)
        assertEquals(2000, bs.year)
        assertEquals(1, bs.month)
        assertEquals(1, bs.day)

        val formattedNe = BikramSambatCalendar.formatBsDate(bs, LanguageCode.NEPALI, devanagariDigits = true)
        assertEquals("१ बैशाख २०००", formattedNe)
    }

    @Test
    fun moneyFormatterSupportsCurrencyAndDevanagari() {
        val npr = MoneyFormatter.format(12500.5, CurrencyCode.NPR, devanagariDigits = false)
        assertEquals("रु 12,500.50", npr)

        val nprNe = MoneyFormatter.format(12500.5, CurrencyCode.NPR, devanagariDigits = true)
        assertEquals("रु १२,५००.५०", nprNe)
    }

    @Test
    fun securityCryptoHashesWithSaltAndVerifies() {
        val salt1 = SecurityCrypto.generateSaltHex()
        val salt2 = SecurityCrypto.generateSaltHex()
        assertNotEquals(salt1, salt2)

        val hash1 = SecurityCrypto.hashCredential("4829", salt1)
        val hash2 = SecurityCrypto.hashCredential("4829", salt2)
        assertNotEquals(hash1, hash2)
        assertNotEquals("4829", hash1)

        assertTrue(SecurityCrypto.verifyCredential("4829", salt1, hash1))
        assertFalse(SecurityCrypto.verifyCredential("0000", salt1, hash1))
    }
}
