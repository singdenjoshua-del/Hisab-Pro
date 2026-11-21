package com.example.util

import com.example.model.CurrencyCode
import com.example.model.DateSystem
import com.example.model.LanguageCode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class BsDate(
    val year: Int,
    val month: Int, // 1..12
    val day: Int    // 1..32
)

/**
 * Deterministic Gregorian (AD) <-> Bikram Sambat (BS) calendar converter
 * covering BS years 2000..2099 (approx. 1943..2043 AD).
 */
object BikramSambatCalendar {
    private const val START_BS_YEAR = 2000
    // Reference: 2000-01-01 BS == 1943-04-14 AD
    private val refAdCal: Calendar = Calendar.getInstance().apply {
        set(1943, Calendar.APRIL, 14, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Days in each BS month (Baisakh..Chaitra) from 2000 BS to 2099 BS
    private val bsMonthDays = arrayOf(
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2000
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2001
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2002
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2003
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2004
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2005
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2006
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2007
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2008
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2009
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2010
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2011
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2012
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2013
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2014
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2015
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2016
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2017
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2018
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2019
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2020
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2021
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2022
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2023
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2024
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2025
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2026
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2027
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2028
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30), // 2029
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2030
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2031
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2032
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2033
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2034
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2035
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2036
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2037
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2038
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2039
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2040
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2041
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2042
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2043
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2044
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2045
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2046
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2047
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2048
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2049
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2050
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2051
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2052
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2053
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2054
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2055
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30), // 2056
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2057
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2058
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2059
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2060
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2061
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31), // 2062
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2063
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2064
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2065
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2066
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2067
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2068
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2069
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2070
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2071
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2072
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2073
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2074
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2075
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2076
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2077
        intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30), // 2078
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2079
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30), // 2080
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2081
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2082
        intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30), // 2083
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2084
        intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31), // 2085
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2086
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2087
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2088
        intArrayOf(30, 32, 31, 32, 31, 31, 29, 30, 29, 30, 29, 31), // 2089
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2090
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2091
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2092
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31), // 2093
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2094
        intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30), // 2095
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31), // 2096
        intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30), // 2097
        intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30), // 2098
        intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30)  // 2099
    )

    val bsMonthsEn = listOf(
        "Baisakh", "Jestha", "Ashadh", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    val bsMonthsNe = listOf(
        "बैशाख", "जेठ", "असार", "साउन", "भदौ", "असोज",
        "कार्तिक", "मंसिर", "पुष", "माघ", "फागुन", "चैत"
    )

    fun toBs(epochMs: Long): BsDate {
        val target = Calendar.getInstance().apply {
            timeInMillis = epochMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = ((target.timeInMillis - refAdCal.timeInMillis) / 86_400_000L).toInt()
        if (diffDays <= 0) return BsDate(START_BS_YEAR, 1, 1)

        var remaining = diffDays
        for (yearOffset in bsMonthDays.indices) {
            val yearDays = bsMonthDays[yearOffset]
            for (m in 0..11) {
                val md = yearDays[m]
                if (remaining < md) {
                    return BsDate(START_BS_YEAR + yearOffset, m + 1, remaining + 1)
                }
                remaining -= md
            }
        }
        return BsDate(START_BS_YEAR + bsMonthDays.size - 1, 12, 30)
    }

    fun formatBsDate(bs: BsDate, language: LanguageCode, devanagariDigits: Boolean): String {
        val monthName = if (language == LanguageCode.NEPALI) {
            bsMonthsNe[bs.month - 1]
        } else {
            bsMonthsEn[bs.month - 1]
        }
        val raw = "${bs.day} $monthName ${bs.year}"
        return if (devanagariDigits) toDevanagariDigits(raw) else raw
    }

    fun formatAdDate(epochMs: Long, language: LanguageCode, devanagariDigits: Boolean): String {
        val fmt = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
        val raw = fmt.format(Date(epochMs))
        return if (devanagariDigits && language == LanguageCode.NEPALI) toDevanagariDigits(raw) else raw
    }

    fun formatDisplayDate(
        epochMs: Long,
        dateSystem: DateSystem,
        language: LanguageCode,
        devanagariDigits: Boolean
    ): String {
        val bsText = formatBsDate(toBs(epochMs), language, devanagariDigits)
        val adText = formatAdDate(epochMs, language, devanagariDigits)
        return when (dateSystem) {
            DateSystem.BS -> bsText
            DateSystem.AD -> adText
            DateSystem.BOTH -> "$bsText ($adText)"
        }
    }

    fun isSameDay(epochA: Long, epochB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = epochA }
        val calB = Calendar.getInstance().apply { timeInMillis = epochB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameBsMonth(epochA: Long, epochB: Long): Boolean {
        val bsA = toBs(epochA)
        val bsB = toBs(epochB)
        return bsA.year == bsB.year && bsA.month == bsB.month
    }

    fun isSameAdMonth(epochA: Long, epochB: Long): Boolean {
        val calA = Calendar.getInstance().apply { timeInMillis = epochA }
        val calB = Calendar.getInstance().apply { timeInMillis = epochB }
        return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
            calA.get(Calendar.MONTH) == calB.get(Calendar.MONTH)
    }

    fun toDevanagariDigits(input: String): String {
        val map = charArrayOf('०', '१', '२', '३', '४', '५', '६', '७', '८', '९')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') sb.append(map[ch - '0']) else sb.append(ch)
        }
        return sb.toString()
    }
}

object MoneyFormatter {
    fun format(
        amount: Double,
        currency: CurrencyCode,
        devanagariDigits: Boolean = false,
        showSign: Boolean = false
    ): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        val pattern = if (currency == CurrencyCode.JPY) "#,##0" else "#,##0.00"
        val df = DecimalFormat(pattern, symbols)
        val absFormatted = df.format(abs(amount))
        val prefix = when {
            amount < 0 -> "-${currency.symbol} "
            showSign && amount > 0 -> "+${currency.symbol} "
            else -> "${currency.symbol} "
        }
        val combined = "$prefix$absFormatted"
        return if (devanagariDigits) BikramSambatCalendar.toDevanagariDigits(combined) else combined
    }

    fun formatCompact(
        amount: Double,
        currency: CurrencyCode,
        devanagariDigits: Boolean = false
    ): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        val numText = when {
            absVal >= 10_000_000 -> String.format(Locale.ENGLISH, "%.2fCr", absVal / 10_000_000)
            absVal >= 100_000 -> String.format(Locale.ENGLISH, "%.2fL", absVal / 100_000)
            absVal >= 1_000 -> String.format(Locale.ENGLISH, "%.1fK", absVal / 1_000)
            else -> String.format(Locale.ENGLISH, "%.0f", absVal)
        }
        val combined = "$sign${currency.symbol}$numText"
        return if (devanagariDigits) BikramSambatCalendar.toDevanagariDigits(combined) else combined
    }
}
