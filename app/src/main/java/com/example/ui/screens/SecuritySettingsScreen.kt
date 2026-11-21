package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.LocalSecurityPreferences
import com.example.i18n.AppStrings
import com.example.model.AuthLockMethod
import com.example.model.AutoLockTimeout
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GlassTextField
import com.example.ui.theme.LocalHisabColors
import com.example.util.SecurityCrypto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    prefs: LocalSecurityPreferences,
    strings: AppStrings,
    onBack: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onSelectPrimaryMethod: (AuthLockMethod) -> Unit,
    onSelectFallbackMethod: (AuthLockMethod) -> Unit,
    onSelectAutoLockTimeout: (AutoLockTimeout) -> Unit,
    onSavePin: (String) -> Unit,
    onSavePattern: (List<Int>) -> Unit,
    onSavePassword: (String) -> Unit,
    onLockNow: () -> Unit
) {
    val context = LocalContext.current
    val extra = LocalHisabColors.current
    val canBiometric = remember(context) { SecurityCrypto.canUseBiometrics(context) }

    var showPinDialog by remember { mutableStateOf(false) }
    var showPatternDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var statusTip by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(strings.securitySettingsTitle, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("security_back_btn")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.cancel)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("security_settings_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.enableAppLock,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.appLockSubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.appLockEnabled,
                            onCheckedChange = { enable ->
                                if (enable && !prefs.isMethodConfigured(prefs.primaryMethod)) {
                                    when (prefs.primaryMethod) {
                                        AuthLockMethod.PIN -> showPinDialog = true
                                        AuthLockMethod.PATTERN -> showPatternDialog = true
                                        AuthLockMethod.PASSWORD -> showPasswordDialog = true
                                        else -> {
                                            if (!prefs.hasAnySecretFallback()) {
                                                showPinDialog = true
                                            } else {
                                                onToggleAppLock(true)
                                            }
                                        }
                                    }
                                } else {
                                    onToggleAppLock(enable)
                                }
                            },
                            modifier = Modifier.testTag("app_lock_switch")
                        )
                    }
                }
            }

            if (statusTip != null) {
                item {
                    Text(
                        text = statusTip ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Primary Unlock Method
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.activeUnlockMethod,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val methods = listOf(
                            Triple(AuthLockMethod.FINGERPRINT, strings.fingerprintLabel, Icons.Rounded.Fingerprint),
                            Triple(AuthLockMethod.FACE, strings.faceLabel, Icons.Rounded.Face),
                            Triple(AuthLockMethod.PATTERN, strings.patternLabel, Icons.Rounded.Gesture),
                            Triple(AuthLockMethod.PIN, strings.pinLabel, Icons.Rounded.Pin),
                            Triple(AuthLockMethod.PASSWORD, strings.passwordLabel, Icons.Rounded.Password)
                        )
                        methods.forEach { (method, label, icon) ->
                            val isSelected = prefs.primaryMethod == method
                            val configured = prefs.isMethodConfigured(method)
                            val biometricDisabled = (method == AuthLockMethod.FINGERPRINT || method == AuthLockMethod.FACE) && !canBiometric

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (biometricDisabled) {
                                            statusTip = strings.biometricUnavailable
                                            return@clickable
                                        }
                                        statusTip = null
                                        if (!configured) {
                                            when (method) {
                                                AuthLockMethod.PIN -> showPinDialog = true
                                                AuthLockMethod.PATTERN -> showPatternDialog = true
                                                AuthLockMethod.PASSWORD -> showPasswordDialog = true
                                                else -> Unit
                                            }
                                        } else {
                                            onSelectPrimaryMethod(method)
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                    .testTag("primary_method_${method.wire}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = extra.accentColor)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                    if (method in listOf(AuthLockMethod.PIN, AuthLockMethod.PATTERN, AuthLockMethod.PASSWORD)) {
                                        Text(
                                            text = if (configured) strings.configuredLabel else strings.notConfiguredLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (configured) extra.incomeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        if (biometricDisabled) {
                                            statusTip = strings.biometricUnavailable
                                        } else if (!configured) {
                                            when (method) {
                                                AuthLockMethod.PIN -> showPinDialog = true
                                                AuthLockMethod.PATTERN -> showPatternDialog = true
                                                AuthLockMethod.PASSWORD -> showPasswordDialog = true
                                                else -> Unit
                                            }
                                        } else {
                                            onSelectPrimaryMethod(method)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Configure Secret Credentials
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassOutlinedButton(
                            onClick = { showPinDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("configure_pin_btn")
                        ) {
                            Icon(Icons.Rounded.Pin, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.configurePin)
                        }
                        GlassOutlinedButton(
                            onClick = { showPatternDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("configure_pattern_btn")
                        ) {
                            Icon(Icons.Rounded.Gesture, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.configurePattern)
                        }
                        GlassOutlinedButton(
                            onClick = { showPasswordDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("configure_password_btn")
                        ) {
                            Icon(Icons.Rounded.Password, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.configurePassword)
                        }
                    }
                }
            }

            // Fallback & Auto-Lock
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.fallbackMethod,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                AuthLockMethod.PIN to strings.pinLabel,
                                AuthLockMethod.PATTERN to strings.patternLabel,
                                AuthLockMethod.PASSWORD to strings.passwordLabel
                            ).forEach { (m, lbl) ->
                                FilterChip(
                                    selected = prefs.fallbackMethod == m,
                                    onClick = { onSelectFallbackMethod(m) },
                                    label = { Text(lbl) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.autoLockTimer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        val timeouts = listOf(
                            AutoLockTimeout.IMMEDIATE to strings.immediate,
                            AutoLockTimeout.SECONDS_30 to strings.seconds30,
                            AutoLockTimeout.MINUTE_1 to strings.minute1,
                            AutoLockTimeout.MINUTES_5 to strings.minutes5,
                            AutoLockTimeout.NEVER to strings.never
                        )
                        timeouts.forEach { (t, lbl) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectAutoLockTimeout(t) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(lbl, style = MaterialTheme.typography.bodyLarge)
                                if (prefs.autoLockTimeout == t) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = extra.accentColor)
                                }
                            }
                        }
                    }
                }
            }

            if (prefs.appLockEnabled) {
                item {
                    GlassButton(
                        onClick = onLockNow,
                        accentColor = extra.accentColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .testTag("lock_now_btn")
                    ) {
                        Icon(Icons.Rounded.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.lockAppNow, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showPinDialog) {
        SetupPinDialog(
            strings = strings,
            onDismiss = { showPinDialog = false },
            onSave = { pin ->
                onSavePin(pin)
                onSelectPrimaryMethod(AuthLockMethod.PIN)
                onToggleAppLock(true)
                showPinDialog = false
            }
        )
    }

    if (showPatternDialog) {
        SetupPatternDialog(
            strings = strings,
            onDismiss = { showPatternDialog = false },
            onSave = { pattern ->
                onSavePattern(pattern)
                onSelectPrimaryMethod(AuthLockMethod.PATTERN)
                onToggleAppLock(true)
                showPatternDialog = false
            }
        )
    }

    if (showPasswordDialog) {
        SetupPasswordDialog(
            strings = strings,
            onDismiss = { showPasswordDialog = false },
            onSave = { password ->
                onSavePassword(password)
                onSelectPrimaryMethod(AuthLockMethod.PASSWORD)
                onToggleAppLock(true)
                showPasswordDialog = false
            }
        )
    }
}

@Composable
private fun SetupPinDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin1 by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.configurePin, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = pin1,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin1 = it },
                    label = { Text(strings.enterNewPinPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = pin2,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin2 = it },
                    label = { Text(strings.confirmPinPrompt) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    when {
                        pin1.length < 4 -> error = strings.enterNewPinPrompt
                        pin1 != pin2 -> error = strings.wrongCredential
                        else -> onSave(pin1)
                    }
                },
                modifier = Modifier.testTag("save_pin_confirm_btn")
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
private fun SetupPatternDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (List<Int>) -> Unit
) {
    var firstPattern by remember { mutableStateOf<List<Int>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.configurePattern, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (firstPattern == null) strings.drawNewPatternPrompt else strings.confirmPatternPrompt,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                PatternLockGrid(
                    onPatternFinished = { drawn ->
                        if (drawn.size < 4) {
                            error = strings.patternTooShort
                            return@PatternLockGrid
                        }
                        val first = firstPattern
                        if (first == null) {
                            firstPattern = drawn
                            error = null
                        } else if (first == drawn) {
                            onSave(drawn)
                        } else {
                            firstPattern = null
                            error = strings.patternMismatch
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun SetupPasswordDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pass1 by remember { mutableStateOf("") }
    var pass2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.configurePassword, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassTextField(
                    value = pass1,
                    onValueChange = { pass1 = it },
                    label = { Text(strings.passwordLabel) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                GlassTextField(
                    value = pass2,
                    onValueChange = { pass2 = it },
                    label = { Text(strings.confirmPinPrompt) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            GlassButton(
                onClick = {
                    when {
                        pass1.length < 4 -> error = strings.enterPasswordPrompt
                        pass1 != pass2 -> error = strings.wrongCredential
                        else -> onSave(pass1)
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
