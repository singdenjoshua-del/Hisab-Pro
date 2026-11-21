package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AccountRecord
import com.example.model.AccountType
import com.example.model.CategoryRecord
import com.example.model.CategoryType
import com.example.model.CustomerRecord
import com.example.model.TransactionRecord
import com.example.model.TransactionType
import com.example.model.UserSettingsRecord
import com.example.util.BackupPayload
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.persistentCacheSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class HisabRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        createConfiguredFirestore(context.applicationContext)
    )

    companion object {
        private fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
            val dbId = appContext.getString(R.string.firestore_database_id)
            val instance = FirebaseFirestore.getInstance(dbId)
            try {
                instance.firestoreSettings = firestoreSettings {
                    setLocalCacheSettings(persistentCacheSettings {})
                }
            } catch (_: IllegalStateException) {
                // Settings already configured on this singleton instance
            }
            return instance
        }

        private const val OFFLINE_COMMIT_WAIT_MS = 1800L
    }

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeAccounts(userId: String): Flow<List<AccountRecord>> = flow {
        val path = "users/$userId/accounts"
        emitAll(
            userDoc(userId).collection("accounts")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AccountRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCategories(userId: String): Flow<List<CategoryRecord>> = flow {
        val path = "users/$userId/categories"
        emitAll(
            userDoc(userId).collection("categories")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CategoryRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.nameEn.lowercase() }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeCustomers(userId: String): Flow<List<CustomerRecord>> = flow {
        val path = "users/$userId/customers"
        emitAll(
            userDoc(userId).collection("customers")
                .snapshots()
                .map { snap ->
                    snap.toObjects(CustomerRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { kotlin.math.abs(it.netBalance) }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeTransactions(userId: String): Flow<List<TransactionRecord>> = flow {
        val path = "users/$userId/transactions"
        emitAll(
            userDoc(userId).collection("transactions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(TransactionRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.transactionEpochMs }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.LIST, path, userId)
                    throw error
                }
        )
    }

    fun observeSettings(userId: String): Flow<UserSettingsRecord?> = flow {
        val path = "users/$userId/settings/preferences"
        emitAll(
            userDoc(userId).collection("settings").document("preferences")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserSettingsRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    handleFlowError(error, OperationType.GET, path, userId)
                    throw error
                }
        )
    }

    private fun handleFlowError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    suspend fun seedDefaultsIfEmpty(
        userId: String,
        initialLanguageCode: String,
        languageSelected: Boolean
    ): Result<Unit> {
        val basePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed." }

            val batch = db.batch()
            var hasWrites = false

            val accSnap = userDoc(uid).collection("accounts").limit(1).get().await()
            if (accSnap.isEmpty) {
                hasWrites = true
                val defaultAccounts = listOf(
                    Triple("acc_cash", "Cash Wallet", AccountType.CASH.wire) to Pair("wallet", "#10B981"),
                    Triple("acc_bank", "Primary Bank", AccountType.BANK.wire) to Pair("bank", "#38BDF8"),
                    Triple("acc_wallet", "Digital Wallet", AccountType.WALLET.wire) to Pair("smartphone", "#8B5CF6")
                )
                defaultAccounts.forEach { (idNameType, iconColor) ->
                    val (id, name, type) = idNameType
                    val (icon, color) = iconColor
                    batch.set(
                        userDoc(uid).collection("accounts").document(id),
                        mapOf(
                            "userId" to uid,
                            "name" to name,
                            "accountType" to type,
                            "balance" to 0.0,
                            "iconKey" to icon,
                            "colorHex" to color,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val catSnap = userDoc(uid).collection("categories").limit(1).get().await()
            if (catSnap.isEmpty) {
                hasWrites = true
                val defaults = listOf(
                    listOf("cat_food", "Food & Dining", "खाना र खाजा", CategoryType.EXPENSE.wire, "restaurant", "#F59E0B"),
                    listOf("cat_transport", "Transport", "यातायात", CategoryType.EXPENSE.wire, "directions_car", "#38BDF8"),
                    listOf("cat_bills", "Bills & Utilities", "बिल र बिजुली", CategoryType.EXPENSE.wire, "receipt_long", "#F43F5E"),
                    listOf("cat_shopping", "Shopping", "किनमेल", CategoryType.EXPENSE.wire, "shopping_bag", "#EC4899"),
                    listOf("cat_health", "Health", "स्वास्थ्य", CategoryType.EXPENSE.wire, "medical_services", "#EF4444"),
                    listOf("cat_edu", "Education", "शिक्षा", CategoryType.EXPENSE.wire, "school", "#8B5CF6"),
                    listOf("cat_salary", "Salary", "तलब", CategoryType.INCOME.wire, "payments", "#10B981"),
                    listOf("cat_business", "Business", "व्यापार", CategoryType.INCOME.wire, "storefront", "#22C55E"),
                    listOf("cat_freelance", "Freelance", "फ्रीलान्स", CategoryType.INCOME.wire, "laptop", "#06B6D4"),
                    listOf("cat_other", "Other", "अन्य", CategoryType.EXPENSE.wire, "category", "#94A3B8")
                )
                defaults.forEach { item ->
                    batch.set(
                        userDoc(uid).collection("categories").document(item[0]),
                        mapOf(
                            "userId" to uid,
                            "nameEn" to item[1],
                            "nameNe" to item[2],
                            "type" to item[3],
                            "iconKey" to item[4],
                            "colorHex" to item[5],
                            "systemDefault" to true,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val prefRef = userDoc(uid).collection("settings").document("preferences")
            val prefSnap = prefRef.get().await()
            if (!prefSnap.exists()) {
                hasWrites = true
                batch.set(
                    prefRef,
                    mapOf(
                        "userId" to uid,
                        "languageCode" to initialLanguageCode,
                        "dateSystem" to "BS",
                        "currencyCode" to "NPR",
                        "themeId" to "midnight_black",
                        "accentColorHex" to "#10B981",
                        "appearanceMode" to "DARK",
                        "backgroundStyle" to "GRADIENT",
                        "monthlyBudget" to 50000.0,
                        "devanagariDigits" to (initialLanguageCode == "ne"),
                        "languageSelected" to languageSelected,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (hasWrites) {
                withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                    batch.commit().await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    suspend fun saveSettings(userId: String, settings: UserSettingsRecord): Result<Unit> {
        val docPath = "users/$userId/settings/preferences"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("settings").document("preferences")
            val payload = mapOf(
                "languageCode" to settings.languageCode,
                "dateSystem" to settings.dateSystem,
                "currencyCode" to settings.currencyCode,
                "themeId" to settings.themeId.take(40),
                "accentColorHex" to settings.accentColorHex.take(16),
                "appearanceMode" to settings.appearanceMode,
                "backgroundStyle" to settings.backgroundStyle,
                "monthlyBudget" to settings.monthlyBudget.coerceAtLeast(0.0),
                "devanagariDigits" to settings.devanagariDigits,
                "languageSelected" to settings.languageSelected,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                val snap = docRef.get().await()
                if (snap.exists()) {
                    docRef.update(payload).await()
                } else {
                    docRef.set(
                        payload + mapOf(
                            "userId" to uid,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveAccount(userId: String, account: AccountRecord): Result<Unit> {
        val isNew = account.id.isBlank()
        val docRef = if (isNew) {
            userDoc(userId).collection("accounts").document()
        } else {
            userDoc(userId).collection("accounts").document(account.id)
        }
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = account.name.trim().take(60)
            require(cleanName.isNotEmpty()) { "Account name cannot be blank" }

            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                if (isNew) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "name" to cleanName,
                            "accountType" to account.accountType,
                            "balance" to account.balance,
                            "iconKey" to account.iconKey.take(40),
                            "colorHex" to account.colorHex.take(16),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.update(
                        mapOf(
                            "name" to cleanName,
                            "accountType" to account.accountType,
                            "balance" to account.balance,
                            "iconKey" to account.iconKey.take(40),
                            "colorHex" to account.colorHex.take(16),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(userId: String, accountId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("accounts").document(accountId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveCustomer(userId: String, customer: CustomerRecord): Result<Unit> {
        val isNew = customer.id.isBlank()
        val docRef = if (isNew) {
            userDoc(userId).collection("customers").document()
        } else {
            userDoc(userId).collection("customers").document(customer.id)
        }
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanName = customer.name.trim().take(80)
            require(cleanName.isNotEmpty()) { "Customer name cannot be blank" }

            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                if (isNew) {
                    docRef.set(
                        mapOf(
                            "userId" to uid,
                            "name" to cleanName,
                            "phone" to customer.phone.trim().take(30),
                            "note" to customer.note.trim().take(300),
                            "netBalance" to customer.netBalance,
                            "totalGiven" to customer.totalGiven.coerceAtLeast(0.0),
                            "totalReceived" to customer.totalReceived.coerceAtLeast(0.0),
                            "colorHex" to customer.colorHex.take(16),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    docRef.update(
                        mapOf(
                            "name" to cleanName,
                            "phone" to customer.phone.trim().take(30),
                            "note" to customer.note.trim().take(300),
                            "colorHex" to customer.colorHex.take(16),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Result<Unit> {
        val docRef = userDoc(userId).collection("customers").document(customerId)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    /**
     * Atomically creates or updates a transaction AND adjusts the linked account and customer balances
     * using a Firestore WriteBatch so financial ledgers remain strictly consistent online and offline.
     */
    suspend fun saveTransactionAtomic(
        userId: String,
        tx: TransactionRecord,
        previousTx: TransactionRecord?,
        currentAccounts: List<AccountRecord>,
        currentCustomers: List<CustomerRecord>
    ): Result<Unit> {
        val isNew = tx.id.isBlank()
        val txRef = if (isNew) {
            userDoc(userId).collection("transactions").document()
        } else {
            userDoc(userId).collection("transactions").document(tx.id)
        }
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val cleanTitle = tx.title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "Title cannot be blank" }
            require(tx.amount > 0.0) { "Amount must be greater than zero" }
            require(tx.accountId.isNotBlank()) { "Account must be selected" }

            val batch = db.batch()
            val accountDeltas = mutableMapOf<String, Double>()
            val customerBalanceDeltas = mutableMapOf<String, Double>()
            val customerGivenDeltas = mutableMapOf<String, Double>()
            val customerReceivedDeltas = mutableMapOf<String, Double>()

            // Revert previous transaction effects if editing
            if (previousTx != null) {
                val prevSign = accountEffectSign(TransactionType.fromWire(previousTx.type))
                if (prevSign != 0.0 && previousTx.accountId.isNotBlank()) {
                    accountDeltas[previousTx.accountId] =
                        (accountDeltas[previousTx.accountId] ?: 0.0) - (prevSign * previousTx.amount)
                }
                if (previousTx.customerId.isNotBlank()) {
                    when (TransactionType.fromWire(previousTx.type)) {
                        TransactionType.CUSTOMER_GAVE -> {
                            customerBalanceDeltas[previousTx.customerId] =
                                (customerBalanceDeltas[previousTx.customerId] ?: 0.0) - previousTx.amount
                            customerGivenDeltas[previousTx.customerId] =
                                (customerGivenDeltas[previousTx.customerId] ?: 0.0) - previousTx.amount
                        }
                        TransactionType.CUSTOMER_RECEIVED -> {
                            customerBalanceDeltas[previousTx.customerId] =
                                (customerBalanceDeltas[previousTx.customerId] ?: 0.0) + previousTx.amount
                            customerReceivedDeltas[previousTx.customerId] =
                                (customerReceivedDeltas[previousTx.customerId] ?: 0.0) - previousTx.amount
                        }
                        else -> Unit
                    }
                }
            }

            // Apply new transaction effects
            val newType = TransactionType.fromWire(tx.type)
            val newSign = accountEffectSign(newType)
            if (newSign != 0.0) {
                accountDeltas[tx.accountId] = (accountDeltas[tx.accountId] ?: 0.0) + (newSign * tx.amount)
            }
            if (tx.customerId.isNotBlank()) {
                when (newType) {
                    TransactionType.CUSTOMER_GAVE -> {
                        customerBalanceDeltas[tx.customerId] =
                            (customerBalanceDeltas[tx.customerId] ?: 0.0) + tx.amount
                        customerGivenDeltas[tx.customerId] =
                            (customerGivenDeltas[tx.customerId] ?: 0.0) + tx.amount
                    }
                    TransactionType.CUSTOMER_RECEIVED -> {
                        customerBalanceDeltas[tx.customerId] =
                            (customerBalanceDeltas[tx.customerId] ?: 0.0) - tx.amount
                        customerReceivedDeltas[tx.customerId] =
                            (customerReceivedDeltas[tx.customerId] ?: 0.0) + tx.amount
                    }
                    else -> Unit
                }
            }

            if (isNew) {
                batch.set(
                    txRef,
                    mapOf(
                        "userId" to uid,
                        "title" to cleanTitle,
                        "amount" to tx.amount,
                        "type" to tx.type,
                        "categoryId" to tx.categoryId.take(128),
                        "categoryNameEn" to tx.categoryNameEn.take(60),
                        "categoryNameNe" to tx.categoryNameNe.take(80),
                        "accountId" to tx.accountId.take(128),
                        "accountName" to tx.accountName.take(60),
                        "customerId" to tx.customerId.take(128),
                        "customerName" to tx.customerName.take(80),
                        "note" to tx.note.trim().take(500),
                        "transactionEpochMs" to tx.transactionEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    txRef,
                    mapOf(
                        "title" to cleanTitle,
                        "amount" to tx.amount,
                        "type" to tx.type,
                        "categoryId" to tx.categoryId.take(128),
                        "categoryNameEn" to tx.categoryNameEn.take(60),
                        "categoryNameNe" to tx.categoryNameNe.take(80),
                        "accountId" to tx.accountId.take(128),
                        "accountName" to tx.accountName.take(60),
                        "customerId" to tx.customerId.take(128),
                        "customerName" to tx.customerName.take(80),
                        "note" to tx.note.trim().take(500),
                        "transactionEpochMs" to tx.transactionEpochMs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            accountDeltas.forEach { (accId, delta) ->
                if (delta != 0.0) {
                    val existingAcc = currentAccounts.firstOrNull { it.id == accId }
                    if (existingAcc != null) {
                        batch.update(
                            userDoc(uid).collection("accounts").document(accId),
                            mapOf(
                                "balance" to (existingAcc.balance + delta),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
            }

            val touchedCustomers = customerBalanceDeltas.keys + customerGivenDeltas.keys + customerReceivedDeltas.keys
            touchedCustomers.forEach { custId ->
                val existingCust = currentCustomers.firstOrNull { it.id == custId }
                if (existingCust != null) {
                    val newNet = existingCust.netBalance + (customerBalanceDeltas[custId] ?: 0.0)
                    val newGiven = (existingCust.totalGiven + (customerGivenDeltas[custId] ?: 0.0)).coerceAtLeast(0.0)
                    val newReceived = (existingCust.totalReceived + (customerReceivedDeltas[custId] ?: 0.0)).coerceAtLeast(0.0)
                    batch.update(
                        userDoc(uid).collection("customers").document(custId),
                        mapOf(
                            "netBalance" to newNet,
                            "totalGiven" to newGiven,
                            "totalReceived" to newReceived,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTransactionAtomic(
        userId: String,
        tx: TransactionRecord,
        currentAccounts: List<AccountRecord>,
        currentCustomers: List<CustomerRecord>
    ): Result<Unit> {
        val txRef = userDoc(userId).collection("transactions").document(tx.id)
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val batch = db.batch()
            batch.delete(txRef)

            val txType = TransactionType.fromWire(tx.type)
            val sign = accountEffectSign(txType)
            if (sign != 0.0 && tx.accountId.isNotBlank()) {
                val acc = currentAccounts.firstOrNull { it.id == tx.accountId }
                if (acc != null) {
                    batch.update(
                        userDoc(uid).collection("accounts").document(acc.id),
                        mapOf(
                            "balance" to (acc.balance - (sign * tx.amount)),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            if (tx.customerId.isNotBlank()) {
                val cust = currentCustomers.firstOrNull { it.id == tx.customerId }
                if (cust != null) {
                    val (newNet, newGiven, newRec) = when (txType) {
                        TransactionType.CUSTOMER_GAVE -> Triple(
                            cust.netBalance - tx.amount,
                            (cust.totalGiven - tx.amount).coerceAtLeast(0.0),
                            cust.totalReceived
                        )
                        TransactionType.CUSTOMER_RECEIVED -> Triple(
                            cust.netBalance + tx.amount,
                            cust.totalGiven,
                            (cust.totalReceived - tx.amount).coerceAtLeast(0.0)
                        )
                        else -> Triple(cust.netBalance, cust.totalGiven, cust.totalReceived)
                    }
                    batch.update(
                        userDoc(uid).collection("customers").document(cust.id),
                        mapOf(
                            "netBalance" to newNet,
                            "totalGiven" to newGiven,
                            "totalReceived" to newRec,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, txRef.path)
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(userId: String, payload: BackupPayload): Result<Unit> {
        val basePath = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val batch = db.batch()
            var ops = 0

            suspend fun flushIfNeeded() {
                if (ops >= 400) {
                    withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) { batch.commit().await() }
                    ops = 0
                }
            }

            payload.accounts.forEach { acc ->
                val ref = userDoc(uid).collection("accounts").document(acc.id)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "name" to acc.name.take(60),
                        "accountType" to acc.accountType,
                        "balance" to acc.balance,
                        "iconKey" to acc.iconKey.take(40),
                        "colorHex" to acc.colorHex.take(16),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            payload.categories.forEach { cat ->
                val ref = userDoc(uid).collection("categories").document(cat.id)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "nameEn" to cat.nameEn.take(50),
                        "nameNe" to cat.nameNe.take(80),
                        "type" to cat.type,
                        "iconKey" to cat.iconKey.take(40),
                        "colorHex" to cat.colorHex.take(16),
                        "systemDefault" to cat.systemDefault,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            payload.customers.forEach { cust ->
                val ref = userDoc(uid).collection("customers").document(cust.id)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "name" to cust.name.take(80),
                        "phone" to cust.phone.take(30),
                        "note" to cust.note.take(300),
                        "netBalance" to cust.netBalance,
                        "totalGiven" to cust.totalGiven,
                        "totalReceived" to cust.totalReceived,
                        "colorHex" to cust.colorHex.take(16),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            payload.transactions.take(350).forEach { tx ->
                val ref = userDoc(uid).collection("transactions").document(tx.id)
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "title" to tx.title.take(120),
                        "amount" to tx.amount,
                        "type" to tx.type,
                        "categoryId" to tx.categoryId.take(128),
                        "categoryNameEn" to tx.categoryNameEn.take(60),
                        "categoryNameNe" to tx.categoryNameNe.take(80),
                        "accountId" to tx.accountId.take(128),
                        "accountName" to tx.accountName.take(60),
                        "customerId" to tx.customerId.take(128),
                        "customerName" to tx.customerName.take(80),
                        "note" to tx.note.take(500),
                        "transactionEpochMs" to tx.transactionEpochMs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                ops++
            }
            payload.settings?.let { s ->
                val ref = userDoc(uid).collection("settings").document("preferences")
                batch.set(
                    ref,
                    mapOf(
                        "userId" to uid,
                        "languageCode" to s.languageCode,
                        "dateSystem" to s.dateSystem,
                        "currencyCode" to s.currencyCode,
                        "themeId" to s.themeId,
                        "accentColorHex" to s.accentColorHex,
                        "appearanceMode" to s.appearanceMode,
                        "backgroundStyle" to s.backgroundStyle,
                        "monthlyBudget" to s.monthlyBudget,
                        "devanagariDigits" to s.devanagariDigits,
                        "languageSelected" to s.languageSelected,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            withTimeoutOrNull(OFFLINE_COMMIT_WAIT_MS) {
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }

    private fun accountEffectSign(type: TransactionType): Double = when (type) {
        TransactionType.INCOME, TransactionType.CUSTOMER_RECEIVED -> 1.0
        TransactionType.EXPENSE, TransactionType.CUSTOMER_GAVE -> -1.0
    }
}
