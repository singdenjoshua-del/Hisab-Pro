package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AccountType(val wire: String) {
    CASH("CASH"),
    BANK("BANK"),
    WALLET("WALLET"),
    SAVINGS("SAVINGS");

    companion object {
        fun fromWire(value: String?): AccountType =
            entries.firstOrNull { it.wire == value } ?: CASH
    }
}

enum class CategoryType(val wire: String) {
    INCOME("INCOME"),
    EXPENSE("EXPENSE");

    companion object {
        fun fromWire(value: String?): CategoryType =
            entries.firstOrNull { it.wire == value } ?: EXPENSE
    }
}

enum class TransactionType(val wire: String) {
    INCOME("INCOME"),
    EXPENSE("EXPENSE"),
    CUSTOMER_GAVE("CUSTOMER_GAVE"),
    CUSTOMER_RECEIVED("CUSTOMER_RECEIVED");

    companion object {
        fun fromWire(value: String?): TransactionType =
            entries.firstOrNull { it.wire == value } ?: EXPENSE
    }
}

enum class LanguageCode(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    NEPALI("ne", "नेपाली");

    companion object {
        fun fromCode(code: String?): LanguageCode =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class DateSystem(val wire: String) {
    BS("BS"),
    AD("AD"),
    BOTH("BOTH");

    companion object {
        fun fromWire(value: String?): DateSystem =
            entries.firstOrNull { it.wire == value } ?: BS
    }
}

enum class CurrencyCode(
    val code: String,
    val symbol: String,
    val labelEn: String,
    val labelNe: String
) {
    NPR("NPR", "रु", "Nepalese Rupee (NPR)", "नेपाली रुपैयाँ (रु)"),
    USD("USD", "$", "US Dollar (USD)", "अमेरिकी डलर ($)"),
    INR("INR", "₹", "Indian Rupee (INR)", "भारतीय रुपैयाँ (₹)"),
    EUR("EUR", "€", "Euro (EUR)", "युरो (€)"),
    GBP("GBP", "£", "British Pound (GBP)", "पाउन्ड (£)"),
    AED("AED", "د.إ", "UAE Dirham (AED)", "दिर्हाम (AED)"),
    AUD("AUD", "A$", "Australian Dollar (AUD)", "अस्ट्रेलियन डलर (A$)"),
    JPY("JPY", "¥", "Japanese Yen (JPY)", "जापानी येन (¥)");

    companion object {
        fun fromCode(code: String?): CurrencyCode =
            entries.firstOrNull { it.code == code } ?: NPR
    }
}

enum class AppearanceMode(val wire: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    companion object {
        fun fromWire(value: String?): AppearanceMode =
            entries.firstOrNull { it.wire == value } ?: DARK
    }
}

enum class BackgroundStyle(val wire: String) {
    GRADIENT("GRADIENT"),
    FLAT("FLAT"),
    AURORA_GLOW("AURORA_GLOW");

    companion object {
        fun fromWire(value: String?): BackgroundStyle =
            entries.firstOrNull { it.wire == value } ?: GRADIENT
    }
}

enum class AuthLockMethod(val wire: String) {
    FINGERPRINT("FINGERPRINT"),
    FACE("FACE"),
    PATTERN("PATTERN"),
    PIN("PIN"),
    PASSWORD("PASSWORD");

    companion object {
        fun fromWire(value: String?): AuthLockMethod =
            entries.firstOrNull { it.wire == value } ?: PIN
    }
}

enum class AutoLockTimeout(val seconds: Long) {
    IMMEDIATE(0L),
    SECONDS_30(30L),
    MINUTE_1(60L),
    MINUTES_5(300L),
    NEVER(-1L);

    companion object {
        fun fromSeconds(seconds: Long): AutoLockTimeout =
            entries.firstOrNull { it.seconds == seconds } ?: IMMEDIATE
    }
}

@Keep
@IgnoreExtraProperties
data class AccountRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val accountType: String = AccountType.CASH.wire,
    val balance: Double = 0.0,
    val iconKey: String = "wallet",
    val colorHex: String = "#10B981",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CategoryRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val nameEn: String = "",
    val nameNe: String = "",
    val type: String = CategoryType.EXPENSE.wire,
    val iconKey: String = "category",
    val colorHex: String = "#F59E0B",
    val systemDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomerRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val note: String = "",
    val netBalance: Double = 0.0,
    val totalGiven: Double = 0.0,
    val totalReceived: Double = 0.0,
    val colorHex: String = "#3B82F6",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TransactionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val type: String = TransactionType.EXPENSE.wire,
    val categoryId: String = "",
    val categoryNameEn: String = "",
    val categoryNameNe: String = "",
    val accountId: String = "",
    val accountName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val note: String = "",
    val transactionEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserSettingsRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val languageCode: String = LanguageCode.ENGLISH.code,
    val dateSystem: String = DateSystem.BS.wire,
    val currencyCode: String = CurrencyCode.NPR.code,
    val themeId: String = "midnight_black",
    val accentColorHex: String = "#10B981",
    val appearanceMode: String = AppearanceMode.DARK.wire,
    val backgroundStyle: String = BackgroundStyle.GRADIENT.wire,
    val monthlyBudget: Double = 50000.0,
    val devanagariDigits: Boolean = false,
    val languageSelected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
