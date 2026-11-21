package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AuthLockMethod
import com.example.model.AutoLockTimeout
import com.example.model.LanguageCode
import com.example.util.SecurityCrypto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.hisabSecurityDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hisab_pro_security_prefs"
)

data class LocalSecurityPreferences(
    val appLockEnabled: Boolean = false,
    val primaryMethod: AuthLockMethod = AuthLockMethod.PIN,
    val fallbackMethod: AuthLockMethod = AuthLockMethod.PIN,
    val autoLockTimeout: AutoLockTimeout = AutoLockTimeout.IMMEDIATE,
    val pinHash: String = "",
    val pinSalt: String = "",
    val patternHash: String = "",
    val patternSalt: String = "",
    val passwordHash: String = "",
    val passwordSalt: String = "",
    val lastBackgroundEpochMs: Long = 0L,
    val preAuthLanguage: LanguageCode = LanguageCode.ENGLISH,
    val preAuthLanguageSelected: Boolean = false
) {
    fun isMethodConfigured(method: AuthLockMethod): Boolean = when (method) {
        AuthLockMethod.PIN -> pinHash.isNotBlank() && pinSalt.isNotBlank()
        AuthLockMethod.PATTERN -> patternHash.isNotBlank() && patternSalt.isNotBlank()
        AuthLockMethod.PASSWORD -> passwordHash.isNotBlank() && passwordSalt.isNotBlank()
        AuthLockMethod.FINGERPRINT, AuthLockMethod.FACE -> true
    }

    fun hasAnySecretFallback(): Boolean =
        isMethodConfigured(AuthLockMethod.PIN) ||
            isMethodConfigured(AuthLockMethod.PATTERN) ||
            isMethodConfigured(AuthLockMethod.PASSWORD)
}

class LocalSecurityStore(private val context: Context) {
    private val store = context.applicationContext.hisabSecurityDataStore

    private object Keys {
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PRIMARY_METHOD = stringPreferencesKey("primary_method")
        val FALLBACK_METHOD = stringPreferencesKey("fallback_method")
        val AUTO_LOCK_SECONDS = longPreferencesKey("auto_lock_seconds")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PATTERN_HASH = stringPreferencesKey("pattern_hash")
        val PATTERN_SALT = stringPreferencesKey("pattern_salt")
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val LAST_BG_MS = longPreferencesKey("last_bg_ms")
        val PRE_AUTH_LANG = stringPreferencesKey("pre_auth_lang")
        val PRE_AUTH_LANG_SELECTED = booleanPreferencesKey("pre_auth_lang_selected")
    }

    val preferencesFlow: Flow<LocalSecurityPreferences> = store.data.map { prefs ->
        LocalSecurityPreferences(
            appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
            primaryMethod = AuthLockMethod.fromWire(prefs[Keys.PRIMARY_METHOD]),
            fallbackMethod = AuthLockMethod.fromWire(prefs[Keys.FALLBACK_METHOD]),
            autoLockTimeout = AutoLockTimeout.fromSeconds(prefs[Keys.AUTO_LOCK_SECONDS] ?: 0L),
            pinHash = prefs[Keys.PIN_HASH] ?: "",
            pinSalt = prefs[Keys.PIN_SALT] ?: "",
            patternHash = prefs[Keys.PATTERN_HASH] ?: "",
            patternSalt = prefs[Keys.PATTERN_SALT] ?: "",
            passwordHash = prefs[Keys.PASSWORD_HASH] ?: "",
            passwordSalt = prefs[Keys.PASSWORD_SALT] ?: "",
            lastBackgroundEpochMs = prefs[Keys.LAST_BG_MS] ?: 0L,
            preAuthLanguage = LanguageCode.fromCode(prefs[Keys.PRE_AUTH_LANG]),
            preAuthLanguageSelected = prefs[Keys.PRE_AUTH_LANG_SELECTED] ?: false
        )
    }

    suspend fun getSnapshot(): LocalSecurityPreferences = preferencesFlow.first()

    suspend fun setPreAuthLanguage(language: LanguageCode) {
        store.edit { prefs ->
            prefs[Keys.PRE_AUTH_LANG] = language.code
            prefs[Keys.PRE_AUTH_LANG_SELECTED] = true
        }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.APP_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setPrimaryMethod(method: AuthLockMethod) {
        store.edit { prefs ->
            prefs[Keys.PRIMARY_METHOD] = method.wire
        }
    }

    suspend fun setFallbackMethod(method: AuthLockMethod) {
        store.edit { prefs ->
            prefs[Keys.FALLBACK_METHOD] = method.wire
        }
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        store.edit { prefs ->
            prefs[Keys.AUTO_LOCK_SECONDS] = timeout.seconds
        }
    }

    suspend fun savePin(rawPin: String) {
        val salt = SecurityCrypto.generateSaltHex()
        val hash = SecurityCrypto.hashCredential(rawPin, salt)
        store.edit { prefs ->
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun savePattern(patternIndices: List<Int>) {
        val rawPattern = patternIndices.joinToString("-")
        val salt = SecurityCrypto.generateSaltHex()
        val hash = SecurityCrypto.hashCredential(rawPattern, salt)
        store.edit { prefs ->
            prefs[Keys.PATTERN_SALT] = salt
            prefs[Keys.PATTERN_HASH] = hash
        }
    }

    suspend fun savePassword(rawPassword: String) {
        val salt = SecurityCrypto.generateSaltHex()
        val hash = SecurityCrypto.hashCredential(rawPassword, salt)
        store.edit { prefs ->
            prefs[Keys.PASSWORD_SALT] = salt
            prefs[Keys.PASSWORD_HASH] = hash
        }
    }

    suspend fun recordBackgroundTimestamp(epochMs: Long) {
        store.edit { prefs ->
            prefs[Keys.LAST_BG_MS] = epochMs
        }
    }
}
