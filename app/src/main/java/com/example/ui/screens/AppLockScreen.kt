package com.example.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.LocalSecurityPreferences
import com.example.i18n.AppStrings
import com.example.model.AuthLockMethod
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassTextField
import com.example.ui.components.HisabAtmosphericBackground
import com.example.ui.theme.LocalHisabColors
import com.example.util.SecurityCrypto
import kotlin.math.hypot

/**
 * Strict Conditional App Lock UI:
 * Only the currently active authentication method is composed and rendered.
 * No hidden pattern grids, dots, lines, or biometric views exist in the background.
 */
@Composable
fun AppLockScreen(
    activeMethod: AuthLockMethod,
    securityPrefs: LocalSecurityPreferences,
    errorMessage: String?,
    strings: AppStrings,
    onBiometricSuccess: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onVerifyPattern: (List<Int>) -> Boolean,
    onVerifyPassword: (String) -> Boolean,
    onSwitchMethod: (AuthLockMethod) -> Unit
) {
    val availableFallback: AuthLockMethod? = remember(activeMethod, securityPrefs) {
        val preferred = securityPrefs.fallbackMethod
        val candidates = listOf(preferred, AuthLockMethod.PIN, AuthLockMethod.PATTERN, AuthLockMethod.PASSWORD)
        candidates.firstOrNull { it != activeMethod && securityPrefs.isMethodConfigured(it) }
    }

    // Minimal, clean glass backdrop — strictly conditional per selected authentication method
    HisabAtmosphericBackground(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_lock_screen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (activeMethod) {
                AuthLockMethod.FINGERPRINT -> FingerprintOnlyLockScreen(
                    strings = strings,
                    fallbackMethod = availableFallback,
                    onBiometricSuccess = onBiometricSuccess,
                    onSwitchToFallback = { availableFallback?.let(onSwitchMethod) }
                )
                AuthLockMethod.FACE -> FaceOnlyLockScreen(
                    strings = strings,
                    fallbackMethod = availableFallback,
                    onBiometricSuccess = onBiometricSuccess,
                    onSwitchToFallback = { availableFallback?.let(onSwitchMethod) }
                )
                AuthLockMethod.PATTERN -> PatternOnlyLockScreen(
                    strings = strings,
                    errorMessage = errorMessage,
                    fallbackMethod = availableFallback,
                    onPatternComplete = onVerifyPattern,
                    onSwitchToFallback = { availableFallback?.let(onSwitchMethod) }
                )
                AuthLockMethod.PIN -> PinOnlyLockScreen(
                    strings = strings,
                    errorMessage = errorMessage,
                    fallbackMethod = availableFallback,
                    onVerifyPin = onVerifyPin,
                    onSwitchToFallback = { availableFallback?.let(onSwitchMethod) }
                )
                AuthLockMethod.PASSWORD -> PasswordOnlyLockScreen(
                    strings = strings,
                    errorMessage = errorMessage,
                    fallbackMethod = availableFallback,
                    onVerifyPassword = onVerifyPassword,
                    onSwitchToFallback = { availableFallback?.let(onSwitchMethod) }
                )
            }
        }
    }
}

@Composable
private fun FingerprintOnlyLockScreen(
    strings: AppStrings,
    fallbackMethod: AuthLockMethod?,
    onBiometricSuccess: () -> Unit,
    onSwitchToFallback: () -> Unit
) {
    val context = LocalContext.current
    val extra = LocalHisabColors.current
    var localStatus by remember { mutableStateOf<String?>(null) }
    val canBiometric = remember(context) { SecurityCrypto.canUseBiometrics(context) }

    fun launchFingerprintPrompt() {
        val activity = context as? FragmentActivity
        if (activity == null || !canBiometric) {
            localStatus = strings.biometricUnavailable
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        localStatus = errString.toString()
                    }
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strings.appName)
            .setSubtitle(strings.authWithFingerprint)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText(
                if (fallbackMethod != null) strings.useFallbackBtn else strings.cancel
            )
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (canBiometric) launchFingerprintPrompt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .testTag("fingerprint_only_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(extra.accentColor.copy(alpha = 0.12f))
                .border(1.5.dp, extra.accentColor.copy(alpha = 0.5f), CircleShape)
                .clickable { launchFingerprintPrompt() }
                .testTag("fingerprint_scan_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = strings.authWithFingerprint,
                tint = extra.accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.authWithFingerprint,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.authWithFingerprintSub,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (localStatus != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = localStatus ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        GlassButton(
            onClick = { launchFingerprintPrompt() },
            accentColor = extra.accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("fingerprint_prompt_cta")
        ) {
            Text(text = strings.tapToScanFingerprint, fontWeight = FontWeight.SemiBold)
        }

        if (fallbackMethod != null) {
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = onSwitchToFallback,
                modifier = Modifier.testTag("fingerprint_fallback_btn")
            ) {
                Text(
                    text = "${strings.useFallbackBtn}: ${methodLabel(fallbackMethod, strings)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FaceOnlyLockScreen(
    strings: AppStrings,
    fallbackMethod: AuthLockMethod?,
    onBiometricSuccess: () -> Unit,
    onSwitchToFallback: () -> Unit
) {
    val context = LocalContext.current
    val extra = LocalHisabColors.current
    var localStatus by remember { mutableStateOf<String?>(null) }
    val canBiometric = remember(context) { SecurityCrypto.canUseBiometrics(context) }

    fun launchFacePrompt() {
        val activity = context as? FragmentActivity
        if (activity == null || !canBiometric) {
            localStatus = strings.biometricUnavailable
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onBiometricSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        localStatus = errString.toString()
                    }
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strings.appName)
            .setSubtitle(strings.authWithFace)
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .setNegativeButtonText(
                if (fallbackMethod != null) strings.useFallbackBtn else strings.cancel
            )
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (canBiometric) launchFacePrompt()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .testTag("face_only_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(extra.accentColor.copy(alpha = 0.12f))
                .border(1.5.dp, extra.accentColor.copy(alpha = 0.5f), CircleShape)
                .clickable { launchFacePrompt() }
                .testTag("face_scan_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Face,
                contentDescription = strings.authWithFace,
                tint = extra.accentColor,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = strings.authWithFace,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.authWithFaceSub,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (localStatus != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = localStatus ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        GlassButton(
            onClick = { launchFacePrompt() },
            accentColor = extra.accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("face_prompt_cta")
        ) {
            Text(text = strings.tapToScanFace, fontWeight = FontWeight.SemiBold)
        }

        if (fallbackMethod != null) {
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(
                onClick = onSwitchToFallback,
                modifier = Modifier.testTag("face_fallback_btn")
            ) {
                Text(
                    text = "${strings.useFallbackBtn}: ${methodLabel(fallbackMethod, strings)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PatternOnlyLockScreen(
    strings: AppStrings,
    errorMessage: String?,
    fallbackMethod: AuthLockMethod?,
    onPatternComplete: (List<Int>) -> Boolean,
    onSwitchToFallback: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .testTag("pattern_only_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.appName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = strings.drawPatternPrompt,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        GlassCard(
            modifier = Modifier.fillMaxWidth(0.88f),
            shape = RoundedCornerShape(26.dp),
            contentPadding = PaddingValues(14.dp)
        ) {
            PatternLockGrid(
                onPatternFinished = { pattern ->
                    onPatternComplete(pattern)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
        }

        if (fallbackMethod != null) {
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = onSwitchToFallback,
                modifier = Modifier.testTag("pattern_fallback_btn")
            ) {
                Text(
                    text = "${strings.useFallbackBtn}: ${methodLabel(fallbackMethod, strings)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PatternLockGrid(
    onPatternFinished: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val extra = LocalHisabColors.current
    val haptic = LocalHapticFeedback.current
    val selectedDots = remember { mutableStateListOf<Int>() }
    var activeFingerPos by remember { mutableStateOf<Offset?>(null) }
    val dotCenters = remember { Array(9) { Offset.Zero } }
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val activeColor = extra.accentColor

    Box(
        modifier = modifier
            .testTag("pattern_lock_grid")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        selectedDots.clear()
                        activeFingerPos = startOffset
                        val hitRadius = size.width * 0.13f
                        for (i in 0 until 9) {
                            val c = dotCenters[i]
                            if (hypot(startOffset.x - c.x, startOffset.y - c.y) <= hitRadius) {
                                selectedDots.add(i)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                break
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        activeFingerPos = pos
                        val hitRadius = size.width * 0.13f
                        for (i in 0 until 9) {
                            if (i !in selectedDots) {
                                val c = dotCenters[i]
                                if (hypot(pos.x - c.x, pos.y - c.y) <= hitRadius) {
                                    selectedDots.add(i)
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                    },
                    onDragEnd = {
                        activeFingerPos = null
                        if (selectedDots.isNotEmpty()) {
                            onPatternFinished(selectedDots.toList())
                        }
                        selectedDots.clear()
                    },
                    onDragCancel = {
                        activeFingerPos = null
                        selectedDots.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stepX = size.width / 3f
            val stepY = size.height / 3f
            for (row in 0..2) {
                for (col in 0..2) {
                    val idx = row * 3 + col
                    dotCenters[idx] = Offset(
                        x = stepX * col + stepX / 2f,
                        y = stepY * row + stepY / 2f
                    )
                }
            }

            // Connecting lines
            for (i in 0 until selectedDots.size - 1) {
                val from = dotCenters[selectedDots[i]]
                val to = dotCenters[selectedDots[i + 1]]
                drawLine(
                    color = activeColor.copy(alpha = 0.85f),
                    start = from,
                    end = to,
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            val finger = activeFingerPos
            if (finger != null && selectedDots.isNotEmpty()) {
                drawLine(
                    color = activeColor.copy(alpha = 0.45f),
                    start = dotCenters[selectedDots.last()],
                    end = finger,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 9 Dots
            for (i in 0 until 9) {
                val center = dotCenters[i]
                val isSelected = i in selectedDots
                if (isSelected) {
                    drawCircle(
                        color = activeColor.copy(alpha = 0.22f),
                        radius = 22.dp.toPx(),
                        center = center
                    )
                }
                drawCircle(
                    color = if (isSelected) activeColor else idleColor,
                    radius = if (isSelected) 9.dp.toPx() else 6.5.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

@Composable
private fun PinOnlyLockScreen(
    strings: AppStrings,
    errorMessage: String?,
    fallbackMethod: AuthLockMethod?,
    onVerifyPin: (String) -> Boolean,
    onSwitchToFallback: () -> Unit
) {
    val extra = LocalHisabColors.current
    var pinInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 340.dp)
            .testTag("pin_only_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = extra.accentColor,
            modifier = Modifier.size(38.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = strings.enterPinPrompt,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(6) { idx ->
                val filled = idx < pinInput.length
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (filled) extra.accentColor else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("CLR", "0", "OK")
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { key ->
                        when (key) {
                            "CLR" -> {
                                FilledTonalButton(
                                    onClick = {
                                        if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (extra.isDark) 0.55f else 0.78f)
                                    ),
                                    modifier = Modifier
                                        .size(68.dp)
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = if (extra.isDark) 0.18f else 0.8f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                        contentDescription = strings.delete
                                    )
                                }
                            }
                            "OK" -> {
                                GlassButton(
                                    onClick = {
                                        if (pinInput.length >= 4) {
                                            val ok = onVerifyPin(pinInput)
                                            if (!ok) pinInput = ""
                                        }
                                    },
                                    shape = CircleShape,
                                    accentColor = extra.accentColor,
                                    modifier = Modifier
                                        .size(68.dp)
                                        .testTag("pin_unlock_button")
                                ) {
                                    Text(text = "OK", fontWeight = FontWeight.Bold)
                                }
                            }
                            else -> {
                                FilledTonalButton(
                                    onClick = {
                                        if (pinInput.length < 6) {
                                            pinInput += key
                                            if (pinInput.length >= 4) {
                                                // Auto-unlock on match if 4..6 digit PIN matches immediately
                                                if (onVerifyPin(pinInput)) {
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    },
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (extra.isDark) 0.55f else 0.78f)
                                    ),
                                    modifier = Modifier
                                        .size(68.dp)
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = if (extra.isDark) 0.18f else 0.8f),
                                            CircleShape
                                        )
                                        .testTag("pin_key_$key")
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (fallbackMethod != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onSwitchToFallback) {
                Text(
                    text = "${strings.useFallbackBtn}: ${methodLabel(fallbackMethod, strings)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PasswordOnlyLockScreen(
    strings: AppStrings,
    errorMessage: String?,
    fallbackMethod: AuthLockMethod?,
    onVerifyPassword: (String) -> Boolean,
    onSwitchToFallback: () -> Unit
) {
    val extra = LocalHisabColors.current
    var password by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
            .testTag("password_only_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Lock,
            contentDescription = null,
            tint = extra.accentColor,
            modifier = Modifier.size(44.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = strings.enterPasswordPrompt,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        GlassTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(strings.passwordLabel) },
            singleLine = true,
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!onVerifyPassword(password)) password = ""
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_lock_input")
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        GlassButton(
            onClick = {
                if (!onVerifyPassword(password)) password = ""
            },
            accentColor = extra.accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("password_unlock_button")
        ) {
            Text(text = strings.unlockBtn, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }

        if (fallbackMethod != null) {
            Spacer(modifier = Modifier.height(14.dp))
            TextButton(onClick = onSwitchToFallback) {
                Text(
                    text = "${strings.useFallbackBtn}: ${methodLabel(fallbackMethod, strings)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun methodLabel(method: AuthLockMethod, strings: AppStrings): String = when (method) {
    AuthLockMethod.FINGERPRINT -> strings.fingerprintLabel
    AuthLockMethod.FACE -> strings.faceLabel
    AuthLockMethod.PATTERN -> strings.patternLabel
    AuthLockMethod.PIN -> strings.pinLabel
    AuthLockMethod.PASSWORD -> strings.passwordLabel
}
