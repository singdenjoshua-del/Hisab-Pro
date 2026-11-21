package com.example

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AuthManager
import com.example.data.HisabRepository
import com.example.data.LocalSecurityStore
import com.example.i18n.StringsProvider
import com.example.model.AppearanceMode
import com.example.model.BackgroundStyle
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UiState
import com.example.ui.components.HisabAtmosphericBackground
import com.example.ui.components.LanguageOnboardingDialog
import com.example.ui.components.TransactionEditorDialog
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CustomersAndAccountsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SecuritySettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeStoreScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.HisabProTheme
import com.example.ui.theme.LocalHisabColors
import com.example.ui.viewmodel.HisabDashboardData
import com.example.ui.viewmodel.HisabViewModel
import com.example.ui.viewmodel.SecurityViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

private enum class MainTab {
    DASHBOARD,
    TRANSACTIONS,
    LEDGER,
    THEMES,
    SETTINGS
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HisabProAppRoot()
        }
    }
}

@Composable
private fun HisabProAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val securityViewModel: SecurityViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                SecurityViewModel(LocalSecurityStore(app))
            }
        }
    )

    val securityPrefs by securityViewModel.preferences.collectAsStateWithLifecycle()
    val isLocked by securityViewModel.isLocked.collectAsStateWithLifecycle()
    val activeUnlockMethod by securityViewModel.activeUnlockMethod.collectAsStateWithLifecycle()
    val lockError by securityViewModel.authError.collectAsStateWithLifecycle()

    // Lifecycle auto-lock observer (background/foreground + inactivity)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> securityViewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> securityViewModel.onAppForegrounded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(securityPrefs.appLockEnabled, securityPrefs.autoLockTimeout, isLocked) {
        while (securityPrefs.appLockEnabled && securityPrefs.autoLockTimeout.seconds > 0L && !isLocked) {
            delay(5000L)
            securityViewModel.checkInactivityLock()
        }
    }

    // Attempt silent Google Auto-Sign-In on startup
    LaunchedEffect(Unit) {
        AuthManager.attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val currentUser by AuthManager.authStateFlow(Firebase.auth)
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    val preAuthLanguage = securityPrefs.preAuthLanguage
    val preAuthStrings = StringsProvider.forLanguage(preAuthLanguage)

    val user = currentUser
    if (user == null) {
        HisabProTheme(language = preAuthLanguage) {
            Box(modifier = Modifier.fillMaxSize()) {
                AuthScreen(
                    strings = preAuthStrings,
                    currentLanguage = preAuthLanguage,
                    onLanguageToggle = { securityViewModel.setPreAuthLanguage(it) },
                    onAuthSuccess = {}
                )
                if (!securityPrefs.preAuthLanguageSelected) {
                    LanguageOnboardingDialog(
                        strings = preAuthStrings,
                        currentLanguage = preAuthLanguage,
                        onSelectLanguage = { securityViewModel.setPreAuthLanguage(it) },
                        onConfirm = { securityViewModel.setPreAuthLanguage(preAuthLanguage) }
                    )
                }
            }
        }
    } else {
        key(user.uid) {
            AuthenticatedHisabShell(
                user = user,
                securityViewModel = securityViewModel,
                isLocked = isLocked,
                activeUnlockMethod = activeUnlockMethod,
                lockError = lockError,
                onSwitchAccount = {
                    AuthManager.signOut(credentialManager, scope) {
                        AuthManager.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {},
                            onAuthError = {},
                            scope = scope
                        )
                    }
                },
                onSignOut = {
                    AuthManager.signOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedHisabShell(
    user: FirebaseUser,
    securityViewModel: SecurityViewModel,
    isLocked: Boolean,
    activeUnlockMethod: com.example.model.AuthLockMethod,
    lockError: String?,
    onSwitchAccount: () -> Unit,
    onSignOut: () -> Unit
) {
    val hisabViewModel: HisabViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                HisabViewModel(
                    repository = HisabRepository(app),
                    localStore = LocalSecurityStore(app),
                    currentUserId = user.uid
                )
            }
        }
    )

    val uiState by hisabViewModel.uiState.collectAsStateWithLifecycle()
    val securityPrefs by securityViewModel.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        hisabViewModel.events.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val dashboardData = (uiState as? UiState.Success)?.data ?: HisabDashboardData()
    val settings = dashboardData.settings
    val language = LanguageCode.fromCode(settings.languageCode)
    val strings = StringsProvider.forLanguage(language)

    HisabProTheme(
        themeId = settings.themeId,
        accentColorHex = settings.accentColorHex,
        appearanceMode = AppearanceMode.fromWire(settings.appearanceMode),
        backgroundStyle = BackgroundStyle.fromWire(settings.backgroundStyle),
        language = language
    ) {
        if (isLocked) {
            // Strictly render ONLY AppLockScreen when locked
            AppLockScreen(
                activeMethod = activeUnlockMethod,
                securityPrefs = securityPrefs,
                errorMessage = lockError,
                strings = strings,
                onBiometricSuccess = { securityViewModel.onBiometricAuthenticated() },
                onVerifyPin = { securityViewModel.verifyPin(it, strings.wrongCredential) },
                onVerifyPattern = { securityViewModel.verifyPattern(it, strings.wrongCredential) },
                onVerifyPassword = { securityViewModel.verifyPassword(it, strings.wrongCredential) },
                onSwitchMethod = { securityViewModel.switchActiveMethod(it) }
            )
        } else {
            var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
            var showSecuritySettings by remember { mutableStateOf(false) }
            var showTxEditor by remember { mutableStateOf(false) }
            var editingTx by remember { mutableStateOf<TransactionRecord?>(null) }
            var defaultTxType by remember { mutableStateOf(TransactionType.EXPENSE) }
            var defaultTxCustomerId by remember { mutableStateOf("") }

            if (showSecuritySettings) {
                BackHandler { showSecuritySettings = false }
            } else if (currentTab != MainTab.DASHBOARD) {
                BackHandler { currentTab = MainTab.DASHBOARD }
            }

            HisabAtmosphericBackground(
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            securityViewModel.recordUserInteraction()
                        }
                    }
                }
            ) {
                if (showSecuritySettings) {
                    SecuritySettingsScreen(
                        prefs = securityPrefs,
                        strings = strings,
                        onBack = { showSecuritySettings = false },
                        onToggleAppLock = { securityViewModel.setAppLockEnabled(it) },
                        onSelectPrimaryMethod = { securityViewModel.setPrimaryMethod(it) },
                        onSelectFallbackMethod = { securityViewModel.setFallbackMethod(it) },
                        onSelectAutoLockTimeout = { securityViewModel.setAutoLockTimeout(it) },
                        onSavePin = { securityViewModel.savePin(it) },
                        onSavePattern = { securityViewModel.savePattern(it) },
                        onSavePassword = { securityViewModel.savePassword(it) },
                        onLockNow = {
                            showSecuritySettings = false
                            securityViewModel.lockNow()
                        }
                    )
                } else {
                    val extra = LocalHisabColors.current
                    val navItems = listOf(
                        Triple(MainTab.DASHBOARD, strings.navDashboard, Pair(Icons.Rounded.Home, Icons.Outlined.Home)),
                        Triple(MainTab.TRANSACTIONS, strings.navTransactions, Pair(Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong)),
                        Triple(MainTab.LEDGER, strings.navLedger, Pair(Icons.Rounded.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)),
                        Triple(MainTab.THEMES, strings.navThemes, Pair(Icons.Rounded.Palette, Icons.Outlined.Palette)),
                        Triple(MainTab.SETTINGS, strings.navSettings, Pair(Icons.Rounded.Settings, Icons.Outlined.Settings))
                    )

                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val useRail = maxWidth >= 720.dp

                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets.navigationBars,
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            floatingActionButton = {
                                if (currentTab in listOf(MainTab.DASHBOARD, MainTab.TRANSACTIONS, MainTab.LEDGER)) {
                                    val fabShape = RoundedCornerShape(18.dp)
                                    ExtendedFloatingActionButton(
                                        onClick = {
                                            editingTx = null
                                            defaultTxType = TransactionType.EXPENSE
                                            defaultTxCustomerId = ""
                                            showTxEditor = true
                                        },
                                        shape = fabShape,
                                        icon = { Icon(Icons.Rounded.Add, contentDescription = strings.addTransaction) },
                                        text = { Text(strings.addTransaction) },
                                        containerColor = extra.accentColor.copy(alpha = 0.92f),
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .shadow(
                                                elevation = 12.dp,
                                                shape = fabShape,
                                                ambientColor = extra.accentColor.copy(alpha = 0.45f),
                                                spotColor = extra.accentColor.copy(alpha = 0.45f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = Color.White.copy(alpha = 0.42f),
                                                shape = fabShape
                                            )
                                            .testTag("fab_add_transaction")
                                    )
                                }
                            },
                            bottomBar = {
                                if (!useRail) {
                                    val navBgBrush = if (extra.isDark) {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                            )
                                        )
                                    } else {
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.82f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                                            )
                                        )
                                    }
                                    val navBorderColor = if (extra.isDark) {
                                        Color.White.copy(alpha = 0.16f)
                                    } else {
                                        extra.accentColor.copy(alpha = 0.18f)
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(navBgBrush)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(navBorderColor)
                                        )
                                        NavigationBar(
                                            containerColor = Color.Transparent,
                                            tonalElevation = 0.dp,
                                            windowInsets = WindowInsets.navigationBars
                                        ) {
                                            navItems.forEach { (tab, label, icons) ->
                                                val selected = currentTab == tab
                                                NavigationBarItem(
                                                    selected = selected,
                                                    onClick = { currentTab = tab },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selected) icons.first else icons.second,
                                                            contentDescription = label
                                                        )
                                                    },
                                                    label = { Text(label, maxLines = 1) },
                                                    colors = NavigationBarItemDefaults.colors(
                                                        selectedIconColor = extra.accentColor,
                                                        selectedTextColor = extra.accentColor,
                                                        indicatorColor = extra.accentColor.copy(alpha = if (extra.isDark) 0.22f else 0.16f),
                                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(innerPadding)
                            ) {
                                if (useRail) {
                                    val railBgBrush = if (extra.isDark) {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.86f),
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
                                            )
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .background(railBgBrush)
                                    ) {
                                        NavigationRail(
                                            containerColor = Color.Transparent
                                        ) {
                                            navItems.forEach { (tab, label, icons) ->
                                                val selected = currentTab == tab
                                                NavigationRailItem(
                                                    selected = selected,
                                                    onClick = { currentTab = tab },
                                                    icon = {
                                                        Icon(
                                                            imageVector = if (selected) icons.first else icons.second,
                                                            contentDescription = label
                                                        )
                                                    },
                                                    label = { Text(label, maxLines = 1) },
                                                    colors = NavigationRailItemDefaults.colors(
                                                        selectedIconColor = extra.accentColor,
                                                        selectedTextColor = extra.accentColor,
                                                        indicatorColor = extra.accentColor.copy(alpha = if (extra.isDark) 0.22f else 0.16f),
                                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .width(1.dp)
                                                .background(
                                                    if (extra.isDark) Color.White.copy(alpha = 0.14f)
                                                    else extra.accentColor.copy(alpha = 0.16f)
                                                )
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    when (val state = uiState) {
                                        is UiState.Loading -> {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(color = extra.accentColor)
                                            }
                                        }
                                        is UiState.Error -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = state.message,
                                                    color = MaterialTheme.colorScheme.error,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                        is UiState.Success -> {
                                            when (currentTab) {
                                                MainTab.DASHBOARD -> DashboardScreen(
                                                    data = state.data,
                                                    strings = strings,
                                                    onViewAllTransactions = { currentTab = MainTab.TRANSACTIONS },
                                                    onEditTransaction = { tx ->
                                                        editingTx = tx
                                                        showTxEditor = true
                                                    },
                                                    onQuickAddIncome = {
                                                        editingTx = null
                                                        defaultTxType = TransactionType.INCOME
                                                        defaultTxCustomerId = ""
                                                        showTxEditor = true
                                                    },
                                                    onQuickAddExpense = {
                                                        editingTx = null
                                                        defaultTxType = TransactionType.EXPENSE
                                                        defaultTxCustomerId = ""
                                                        showTxEditor = true
                                                    },
                                                    onOpenLedger = { currentTab = MainTab.LEDGER },
                                                    onOpenThemes = { currentTab = MainTab.THEMES }
                                                )
                                                MainTab.TRANSACTIONS -> TransactionsScreen(
                                                    data = state.data,
                                                    strings = strings,
                                                    onEditTransaction = { tx ->
                                                        editingTx = tx
                                                        showTxEditor = true
                                                    }
                                                )
                                                MainTab.LEDGER -> CustomersAndAccountsScreen(
                                                    data = state.data,
                                                    strings = strings,
                                                    onSaveCustomer = { hisabViewModel.saveCustomer(it) },
                                                    onDeleteCustomer = { hisabViewModel.deleteCustomer(it) },
                                                    onQuickCustomerTx = { customer, txType ->
                                                        editingTx = null
                                                        defaultTxType = txType
                                                        defaultTxCustomerId = customer.id
                                                        showTxEditor = true
                                                    },
                                                    onSaveAccount = { hisabViewModel.saveAccount(it) },
                                                    onDeleteAccount = { hisabViewModel.deleteAccount(it) }
                                                )
                                                MainTab.THEMES -> ThemeStoreScreen(
                                                    settings = state.data.settings,
                                                    strings = strings,
                                                    onUpdateSettings = { hisabViewModel.updateSettings(it) }
                                                )
                                                MainTab.SETTINGS -> SettingsScreen(
                                                    user = user,
                                                    userId = user.uid,
                                                    data = state.data,
                                                    strings = strings,
                                                    onUpdateSettings = { hisabViewModel.updateSettings(it) },
                                                    onOpenSecuritySettings = { showSecuritySettings = true },
                                                    onSwitchAccount = onSwitchAccount,
                                                    onSignOut = onSignOut,
                                                    onRestoreBackup = { payload ->
                                                        hisabViewModel.restoreBackup(payload, strings.restoreSuccess)
                                                    },
                                                    onMessage = { hisabViewModel.emitMessage(it) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (showTxEditor && uiState is UiState.Success) {
                            val data = (uiState as UiState.Success).data
                            TransactionEditorDialog(
                                initialTx = editingTx,
                                defaultType = defaultTxType,
                                defaultCustomerId = defaultTxCustomerId,
                                accounts = data.accounts,
                                categories = data.categories,
                                customers = data.customers,
                                dateSystem = com.example.model.DateSystem.fromWire(data.settings.dateSystem),
                                language = language,
                                devanagariDigits = data.settings.devanagariDigits,
                                strings = strings,
                                onDismiss = { showTxEditor = false },
                                onSave = { tx ->
                                    hisabViewModel.saveTransaction(tx, editingTx) { ok ->
                                        if (ok) showTxEditor = false
                                    }
                                },
                                onDelete = editingTx?.let { existing ->
                                    {
                                        hisabViewModel.deleteTransaction(existing)
                                        showTxEditor = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
