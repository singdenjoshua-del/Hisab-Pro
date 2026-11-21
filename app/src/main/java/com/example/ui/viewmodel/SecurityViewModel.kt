package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LocalSecurityPreferences
import com.example.data.LocalSecurityStore
import com.example.model.AuthLockMethod
import com.example.model.AutoLockTimeout
import com.example.model.LanguageCode
import com.example.util.SecurityCrypto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SecurityViewModel(
    private val localStore: LocalSecurityStore
) : ViewModel() {

    val preferences: StateFlow<LocalSecurityPreferences> = localStore.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = LocalSecurityPreferences()
        )

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private val _activeUnlockMethod = MutableStateFlow(AuthLockMethod.PIN)
    val activeUnlockMethod: StateFlow<AuthLockMethod> = _activeUnlockMethod.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var initialCheckDone = false
    private var lastActivityInteractionMs = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            val snap = localStore.getSnapshot()
            if (!initialCheckDone) {
                initialCheckDone = true
                if (snap.appLockEnabled && snap.isMethodConfigured(snap.primaryMethod)) {
                    _activeUnlockMethod.value = snap.primaryMethod
                    _isLocked.value = true
                }
            }
        }
    }

    fun recordUserInteraction() {
        lastActivityInteractionMs = System.currentTimeMillis()
        val snap = preferences.value
        val timeout = snap.autoLockTimeout
        if (snap.appLockEnabled && timeout.seconds > 0L && !_isLocked.value) {
            // Checked passively on resume or periodic ticks
        }
    }

    fun checkInactivityLock() {
        val snap = preferences.value
        val timeout = snap.autoLockTimeout
        if (snap.appLockEnabled && timeout.seconds > 0L && !_isLocked.value) {
            val elapsedMs = System.currentTimeMillis() - lastActivityInteractionMs
            if (elapsedMs >= timeout.seconds * 1000L) {
                lockNow()
            }
        }
    }

    fun onAppBackgrounded() {
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            localStore.recordBackgroundTimestamp(now)
        }
    }

    fun onAppForegrounded() {
        val snap = preferences.value
        if (!snap.appLockEnabled) {
            _isLocked.value = false
            return
        }
        val timeout = snap.autoLockTimeout
        if (timeout == AutoLockTimeout.NEVER) return
        val bgAt = snap.lastBackgroundEpochMs
        if (bgAt > 0L) {
            val elapsedMs = System.currentTimeMillis() - bgAt
            if (timeout == AutoLockTimeout.IMMEDIATE || elapsedMs >= timeout.seconds * 1000L) {
                lockNow()
            }
        }
    }

    fun lockNow() {
        val snap = preferences.value
        if (!snap.appLockEnabled) return
        _activeUnlockMethod.value = snap.primaryMethod
        _authError.value = null
        _isLocked.value = true
    }

    fun clearError() {
        _authError.value = null
    }

    fun switchActiveMethod(method: AuthLockMethod) {
        _authError.value = null
        _activeUnlockMethod.value = method
    }

    fun onBiometricAuthenticated() {
        _authError.value = null
        lastActivityInteractionMs = System.currentTimeMillis()
        _isLocked.value = false
    }

    fun verifyPin(pin: String, wrongMsg: String): Boolean {
        val snap = preferences.value
        val ok = SecurityCrypto.verifyCredential(pin, snap.pinSalt, snap.pinHash)
        if (ok) {
            _authError.value = null
            lastActivityInteractionMs = System.currentTimeMillis()
            _isLocked.value = false
        } else {
            _authError.value = wrongMsg
        }
        return ok
    }

    fun verifyPattern(pattern: List<Int>, wrongMsg: String): Boolean {
        val snap = preferences.value
        val raw = pattern.joinToString("-")
        val ok = SecurityCrypto.verifyCredential(raw, snap.patternSalt, snap.patternHash)
        if (ok) {
            _authError.value = null
            lastActivityInteractionMs = System.currentTimeMillis()
            _isLocked.value = false
        } else {
            _authError.value = wrongMsg
        }
        return ok
    }

    fun verifyPassword(password: String, wrongMsg: String): Boolean {
        val snap = preferences.value
        val ok = SecurityCrypto.verifyCredential(password, snap.passwordSalt, snap.passwordHash)
        if (ok) {
            _authError.value = null
            lastActivityInteractionMs = System.currentTimeMillis()
            _isLocked.value = false
        } else {
            _authError.value = wrongMsg
        }
        return ok
    }

    fun setPreAuthLanguage(language: LanguageCode) {
        viewModelScope.launch {
            localStore.setPreAuthLanguage(language)
        }
    }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            localStore.setAppLockEnabled(enabled)
            if (!enabled) {
                _isLocked.value = false
            }
        }
    }

    fun setPrimaryMethod(method: AuthLockMethod) {
        viewModelScope.launch {
            localStore.setPrimaryMethod(method)
        }
    }

    fun setFallbackMethod(method: AuthLockMethod) {
        viewModelScope.launch {
            localStore.setFallbackMethod(method)
        }
    }

    fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        viewModelScope.launch {
            localStore.setAutoLockTimeout(timeout)
        }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            localStore.savePin(pin)
        }
    }

    fun savePattern(pattern: List<Int>) {
        viewModelScope.launch {
            localStore.savePattern(pattern)
        }
    }

    fun savePassword(password: String) {
        viewModelScope.launch {
            localStore.savePassword(password)
        }
    }
}
