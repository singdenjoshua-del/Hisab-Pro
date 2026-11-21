package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HisabRepository
import com.example.data.LocalSecurityStore
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.CategoryRecord
import com.example.model.CustomerRecord
import com.example.model.DateSystem
import com.example.model.LanguageCode
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UiState
import com.example.model.UserSettingsRecord
import com.example.util.BackupPayload
import com.example.util.BikramSambatCalendar
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpendSlice(
    val categoryId: String,
    val labelEn: String,
    val labelNe: String,
    val amount: Double,
    val colorHex: String,
    val percentage: Float
)

data class DailyCashflowBar(
    val dayLabel: String,
    val income: Double,
    val expense: Double
)

data class HisabDashboardData(
    val accounts: List<AccountRecord> = emptyList(),
    val categories: List<CategoryRecord> = emptyList(),
    val customers: List<CustomerRecord> = emptyList(),
    val transactions: List<TransactionRecord> = emptyList(),
    val settings: UserSettingsRecord = UserSettingsRecord(),
    val totalBalance: Double = 0.0,
    val availableBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val todaysExpenses: Double = 0.0,
    val monthlyExpenses: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val totalReceivable: Double = 0.0,
    val totalPayable: Double = 0.0,
    val categorySlices: List<CategorySpendSlice> = emptyList(),
    val weeklyCashflow: List<DailyCashflowBar> = emptyList(),
    val recentTransactions: List<TransactionRecord> = emptyList()
)

class HisabViewModel(
    private val repository: HisabRepository,
    private val localStore: LocalSecurityStore,
    private val currentUserId: String
) : ViewModel() {

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val localPrefs = localStore.getSnapshot()
            repository.seedDefaultsIfEmpty(
                userId = currentUserId,
                initialLanguageCode = localPrefs.preAuthLanguage.code,
                languageSelected = localPrefs.preAuthLanguageSelected
            )
        }
    }

    val uiState: StateFlow<UiState<HisabDashboardData>> = combine(
        repository.observeAccounts(currentUserId),
        repository.observeCategories(currentUserId),
        repository.observeCustomers(currentUserId),
        repository.observeTransactions(currentUserId),
        repository.observeSettings(currentUserId)
    ) { accounts, categories, customers, transactions, remoteSettings ->
        val settings = remoteSettings ?: UserSettingsRecord(id = "preferences", userId = currentUserId)
        val nowMs = System.currentTimeMillis()
        val dateSystem = DateSystem.fromWire(settings.dateSystem)

        val totalBalance = accounts.sumOf { it.balance }
        val availableBalance = accounts
            .filter { AccountType.fromWire(it.accountType) != AccountType.SAVINGS }
            .sumOf { it.balance }

        val incomeTxs = transactions.filter { TransactionType.fromWire(it.type) == TransactionType.INCOME }
        val expenseTxs = transactions.filter { TransactionType.fromWire(it.type) == TransactionType.EXPENSE }

        val totalIncome = incomeTxs.sumOf { it.amount }
        val totalExpenses = expenseTxs.sumOf { it.amount }
        val todaysExpenses = expenseTxs
            .filter { BikramSambatCalendar.isSameDay(it.transactionEpochMs, nowMs) }
            .sumOf { it.amount }

        val isSameCurrentMonth: (Long) -> Boolean = { epoch ->
            if (dateSystem == DateSystem.AD) {
                BikramSambatCalendar.isSameAdMonth(epoch, nowMs)
            } else {
                BikramSambatCalendar.isSameBsMonth(epoch, nowMs)
            }
        }

        val monthlyExpenses = expenseTxs.filter { isSameCurrentMonth(it.transactionEpochMs) }.sumOf { it.amount }
        val monthlyIncome = incomeTxs.filter { isSameCurrentMonth(it.transactionEpochMs) }.sumOf { it.amount }
        val monthlySavings = monthlyIncome - monthlyExpenses

        val totalReceivable = customers.filter { it.netBalance > 0 }.sumOf { it.netBalance }
        val totalPayable = customers.filter { it.netBalance < 0 }.sumOf { -it.netBalance }

        // Category breakdown for expenses
        val expenseSumForChart = totalExpenses.coerceAtLeast(0.0)
        val catLookup = categories.associateBy { it.id }
        val categorySlices = if (expenseSumForChart > 0.0) {
            expenseTxs.groupBy { it.categoryId.ifBlank { "cat_other" } }
                .map { (catId, list) ->
                    val sum = list.sumOf { it.amount }
                    val cat = catLookup[catId]
                    val firstTx = list.first()
                    CategorySpendSlice(
                        categoryId = catId,
                        labelEn = cat?.nameEn ?: firstTx.categoryNameEn.ifBlank { "Other" },
                        labelNe = cat?.nameNe ?: firstTx.categoryNameNe.ifBlank { "अन्य" },
                        amount = sum,
                        colorHex = cat?.colorHex ?: "#94A3B8",
                        percentage = (sum / expenseSumForChart).toFloat().coerceIn(0f, 1f)
                    )
                }
                .sortedByDescending { it.amount }
                .take(6)
        } else {
            emptyList()
        }

        // 7-day cashflow bars
        val dayFmt = SimpleDateFormat("EEE", Locale.ENGLISH)
        val weeklyCashflow = (6 downTo 0).map { daysAgo ->
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dayMs = cal.timeInMillis
            val inc = incomeTxs
                .filter { BikramSambatCalendar.isSameDay(it.transactionEpochMs, dayMs) }
                .sumOf { it.amount }
            val exp = expenseTxs
                .filter { BikramSambatCalendar.isSameDay(it.transactionEpochMs, dayMs) }
                .sumOf { it.amount }
            DailyCashflowBar(
                dayLabel = dayFmt.format(Date(dayMs)),
                income = inc,
                expense = exp
            )
        }

        val data = HisabDashboardData(
            accounts = accounts,
            categories = categories,
            customers = customers,
            transactions = transactions,
            settings = settings,
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            todaysExpenses = todaysExpenses,
            monthlyExpenses = monthlyExpenses,
            monthlyIncome = monthlyIncome,
            monthlySavings = monthlySavings,
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            categorySlices = categorySlices,
            weeklyCashflow = weeklyCashflow,
            recentTransactions = transactions.take(6)
        )
        UiState.Success(data) as UiState<HisabDashboardData>
    }.catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing Hisab Pro streams", rootError)
        emit(UiState.Error(rootError.localizedMessage ?: "Failed to sync cloud data"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun updateSettings(transform: (UserSettingsRecord) -> UserSettingsRecord) {
        val current = (uiState.value as? UiState.Success)?.data?.settings
            ?: UserSettingsRecord(id = "preferences", userId = currentUserId)
        val updated = transform(current)
        viewModelScope.launch {
            if (updated.languageCode != current.languageCode || updated.languageSelected != current.languageSelected) {
                localStore.setPreAuthLanguage(LanguageCode.fromCode(updated.languageCode))
            }
            val result = repository.saveSettings(currentUserId, updated)
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save settings") }
        }
    }

    fun saveAccount(account: AccountRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveAccount(currentUserId, account)
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save account") }
            onComplete(result.isSuccess)
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            val result = repository.deleteAccount(currentUserId, accountId)
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to delete account") }
        }
    }

    fun saveCustomer(customer: CustomerRecord, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.saveCustomer(currentUserId, customer)
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save customer") }
            onComplete(result.isSuccess)
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val result = repository.deleteCustomer(currentUserId, customerId)
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to delete customer") }
        }
    }

    fun saveTransaction(
        tx: TransactionRecord,
        previousTx: TransactionRecord?,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val snapshot = (uiState.value as? UiState.Success)?.data
        val accounts = snapshot?.accounts ?: emptyList()
        val customers = snapshot?.customers ?: emptyList()
        viewModelScope.launch {
            val result = repository.saveTransactionAtomic(
                userId = currentUserId,
                tx = tx,
                previousTx = previousTx,
                currentAccounts = accounts,
                currentCustomers = customers
            )
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to save transaction") }
            onComplete(result.isSuccess)
        }
    }

    fun deleteTransaction(tx: TransactionRecord) {
        val snapshot = (uiState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.deleteTransactionAtomic(
                userId = currentUserId,
                tx = tx,
                currentAccounts = snapshot.accounts,
                currentCustomers = snapshot.customers
            )
            result.onFailure { _events.tryEmit(it.localizedMessage ?: "Failed to delete transaction") }
        }
    }

    fun restoreBackup(payload: BackupPayload, successMessage: String) {
        viewModelScope.launch {
            val result = repository.restoreBackup(currentUserId, payload)
            result.onSuccess { _events.tryEmit(successMessage) }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "Restore failed") }
        }
    }

    fun emitMessage(msg: String) {
        _events.tryEmit(msg)
    }

    private companion object {
        const val TAG = "HisabVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
