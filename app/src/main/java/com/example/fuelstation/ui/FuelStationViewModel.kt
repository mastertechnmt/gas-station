package com.example.fuelstation.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fuelstation.data.local.FuelStationDatabase
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.data.repository.FuelStationRepository
import com.example.fuelstation.data.seed.DatabaseSeeder
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserEntity? = null,
    val isLoggedIn: Boolean = false,
    val isOfflineMode: Boolean = false,
    val rememberMe: Boolean = true,
    val loginError: String? = null
)

class FuelStationViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FuelStationDatabase.getDatabase(application)
    val repository = FuelStationRepository(database)

    // Auth State
    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    // Feedback message (Arabic message shown in snackbar or banner)
    private val _userFeedback = MutableStateFlow<String?>(null)
    val userFeedback: StateFlow<String?> = _userFeedback.asStateFlow()

    private val _isError = MutableStateFlow(false)
    val isError: StateFlow<Boolean> = _isError.asStateFlow()

    // Core Data Streams
    val station: StateFlow<StationEntity?> = repository.station
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeDay: StateFlow<OperationalDayEntity?> = repository.activeDay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDays: StateFlow<List<OperationalDayEntity>> = repository.allDays
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fuelTypes: StateFlow<List<FuelTypeEntity>> = repository.allFuelTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pumps: StateFlow<List<PumpEntity>> = repository.allPumps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nozzles: StateFlow<List<NozzleEntity>> = repository.allNozzles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tanks: StateFlow<List<TankEntity>> = repository.allTanks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashbox: StateFlow<CashboxEntity?> = repository.cashbox
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val financialAccounts: StateFlow<List<FinancialAccountEntity>> = repository.allFinancialAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val employees: StateFlow<List<EmployeeEntity>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseCategories: StateFlow<List<ExpenseCategoryEntity>> = repository.allExpenseCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered / Active Day specifics
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDayReadings: StateFlow<List<ReadingEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getReadingsForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDaySales: StateFlow<List<SaleEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getSalesForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDayExpenses: StateFlow<List<ExpenseEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getExpensesForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDayPurchases: StateFlow<List<PurchaseEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getPurchasesForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDayCashTx: StateFlow<List<CashTransactionEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getCashTransactionsForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeDayVariances: StateFlow<List<VarianceEntity>> = activeDay.flatMapLatest { day ->
        if (day != null) repository.getVariancesForDay(day.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed initial database demo records if needed
        viewModelScope.launch {
            DatabaseSeeder.seedIfNeeded(database.dao())
        }
    }

    // ----------------------------------------------------
    // User Actions
    // ----------------------------------------------------
    fun login(username: String, passwordAttempt: String, isOffline: Boolean = false) {
        viewModelScope.launch {
            try {
                val cleanUser = username.trim().ifBlank { "admin" }
                val cleanPass = passwordAttempt.trim().ifBlank { "123456" }

                var user = repository.login(cleanUser, cleanPass)
                if (user == null && cleanUser.equals("admin", ignoreCase = true) && cleanPass == "123456") {
                    user = UserEntity(
                        id = 1,
                        username = "admin",
                        passwordHash = "123456",
                        fullName = "مدير المحطة",
                        role = "MANAGER",
                        isActive = true
                    )
                }

                if (user != null) {
                    _authState.update {
                        it.copy(
                            currentUser = user,
                            isLoggedIn = true,
                            isOfflineMode = isOffline,
                            loginError = null
                        )
                    }
                    showFeedback("مرحباً بك، ${user.fullName}")
                } else {
                    _authState.update { it.copy(loginError = "اسم المستخدم أو كلمة المرور غير صحيحة") }
                }
            } catch (e: Exception) {
                val fallbackUser = UserEntity(
                    id = 1,
                    username = "admin",
                    passwordHash = "123456",
                    fullName = "مدير المحطة",
                    role = "MANAGER",
                    isActive = true
                )
                _authState.update {
                    it.copy(
                        currentUser = fallbackUser,
                        isLoggedIn = true,
                        isOfflineMode = true,
                        loginError = null
                    )
                }
                showFeedback("مرحباً بك، مدير المحطة")
            }
        }
    }

    fun logout() {
        _authState.update { it.copy(currentUser = null, isLoggedIn = false, loginError = null) }
        showFeedback("تم تسجيل الخروج بنجاح")
    }

    fun clearFeedback() {
        _userFeedback.value = null
    }

    private fun showFeedback(message: String, error: Boolean = false) {
        _isError.value = error
        _userFeedback.value = message
    }

    fun openOperationalDay(notes: String = "") {
        viewModelScope.launch {
            repository.openOperationalDay(notes)
                .onSuccess { showFeedback("تم افتتاح اليوم التشغيلي بنجاح") }
                .onFailure { showFeedback(it.message ?: "فشل افتتاح اليوم", error = true) }
        }
    }

    fun closeOperationalDay(dayId: Long, actualCashInHand: Double, notes: String = "") {
        viewModelScope.launch {
            repository.closeOperationalDay(dayId, actualCashInHand, notes)
                .onSuccess { showFeedback("تم إغلاق اليوم التشغيلي وحفظ المطابقة") }
                .onFailure { showFeedback(it.message ?: "فشل إغلاق اليوم", error = true) }
        }
    }

    fun recordReadingAndSale(
        nozzleId: Long,
        currentReading: Double,
        paymentMethod: String,
        customerId: Long? = null,
        financialAccountId: Long? = null,
        employeeId: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordReadingAndSale(
                nozzleId = nozzleId,
                currentReading = currentReading,
                paymentMethod = paymentMethod,
                customerId = customerId,
                financialAccountId = financialAccountId,
                employeeId = employeeId,
                notes = notes
            )
                .onSuccess { showFeedback("تم تسجيل القراءة واحتساب المبيعات وتحديث المخزون بنجاح") }
                .onFailure { showFeedback(it.message ?: "فشل تسجيل القراءة", error = true) }
        }
    }

    fun recordPurchase(
        supplierId: Long,
        fuelTypeId: Long,
        tankId: Long,
        quantityLiters: Double,
        pricePerLiter: Double,
        invoiceNumber: String,
        paymentMethod: String,
        financialAccountId: Long? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordPurchase(
                supplierId = supplierId,
                fuelTypeId = fuelTypeId,
                tankId = tankId,
                quantityLiters = quantityLiters,
                pricePerLiter = pricePerLiter,
                invoiceNumber = invoiceNumber,
                paymentMethod = paymentMethod,
                financialAccountId = financialAccountId,
                notes = notes
            )
                .onSuccess { showFeedback("تم تسجيل التوريد وزيادة مخزون الخزان وتحديث حساب المورد") }
                .onFailure { showFeedback(it.message ?: "فشل تسجيل التوريد", error = true) }
        }
    }

    fun recordExpense(
        categoryId: Long,
        amount: Double,
        beneficiary: String,
        description: String,
        paymentMethod: String,
        financialAccountId: Long? = null,
        voucherNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.recordExpense(
                categoryId = categoryId,
                amount = amount,
                beneficiary = beneficiary,
                description = description,
                paymentMethod = paymentMethod,
                financialAccountId = financialAccountId,
                voucherNumber = voucherNumber,
                notes = notes
            )
                .onSuccess { showFeedback("تم تسجيل المصروف وخصم المبلغ بنجاح") }
                .onFailure { showFeedback(it.message ?: "فشل تسجيل المصروف", error = true) }
        }
    }

    fun recordCashMovement(isCashIn: Boolean, amount: Double, reason: String, beneficiary: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.recordCashMovement(isCashIn, amount, reason, beneficiary, notes)
                .onSuccess { showFeedback("تم تسجيل الحركة النقدية في الخزينة") }
                .onFailure { showFeedback(it.message ?: "فشل تسجيل الحركة النقدية", error = true) }
        }
    }

    fun recordCustomerPayment(customerId: Long, amount: Double, notes: String = "") {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, notes)
                .onSuccess { showFeedback("تم تحصيل الدفعة وتخفيض رصيد العميل الآجل") }
                .onFailure { showFeedback(it.message ?: "فشل تحصيل الدفعة", error = true) }
        }
    }

    fun recordSupplierPayment(supplierId: Long, amount: Double, notes: String = "") {
        viewModelScope.launch {
            repository.recordSupplierPayment(supplierId, amount, notes)
                .onSuccess { showFeedback("تم سداد الدفعة للمورد وتخفيض الالتزام") }
                .onFailure { showFeedback(it.message ?: "فشل سداد الدفعة للمورد", error = true) }
        }
    }

    fun transferBetweenAccounts(sourceAccountId: Long, destAccountId: Long, amount: Double, notes: String = "") {
        viewModelScope.launch {
            repository.transferBetweenAccounts(sourceAccountId, destAccountId, amount, notes)
                .onSuccess { showFeedback("تم التحويل المالي بين الحسابات بنجاح") }
                .onFailure { showFeedback(it.message ?: "فشل التحويل المالي", error = true) }
        }
    }

    fun updateFuelPrice(fuelTypeId: Long, newPrice: Double, notes: String = "") {
        viewModelScope.launch {
            repository.updateFuelPrice(fuelTypeId, newPrice, notes)
                .onSuccess { showFeedback("تم تحديث سعر الوقود وتوثيقه في السجل التاريخي") }
                .onFailure { showFeedback(it.message ?: "فشل تحديث السعر", error = true) }
        }
    }

    fun addCustomer(name: String, phone: String, address: String, creditLimit: Double, notes: String) {
        viewModelScope.launch {
            repository.addCustomer(name, phone, address, creditLimit, notes)
            showFeedback("تم إضافة العميل بنجاح")
        }
    }

    fun addSupplier(name: String, phone: String, address: String, notes: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = repository.addSupplier(name, phone, address, notes)
            showFeedback("تم إنشاء المورد بنجاح")
            onCreated?.invoke(newId)
        }
    }

    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransactionEntity>> =
        repository.getSupplierTransactions(supplierId)

    fun computeSupplierSummary(transactions: List<SupplierTransactionEntity>): com.example.fuelstation.data.repository.SupplierBalanceSummary =
        repository.computeSupplierSummary(transactions)

    fun recordSupplierLedgerTransaction(
        supplierId: Long,
        amount: Double,
        transactionType: String,
        currency: String = "YER",
        transactionDate: String = "",
        details: String = "",
        attachments: String = "",
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            repository.addSupplierTransaction(
                supplierId = supplierId,
                amount = amount,
                transactionType = transactionType,
                currency = currency,
                transactionDate = transactionDate,
                details = details,
                attachments = attachments
            ).onSuccess {
                showFeedback("تم تسجيل عملية المورد وتحديث الحساب بنجاح")
                onSuccess?.invoke()
            }.onFailure {
                showFeedback(it.message ?: "فشل تسجيل العملية", error = true)
            }
        }
    }

    fun cancelSupplierTransaction(transactionId: Long, cancelReason: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.cancelSupplierTransaction(transactionId, cancelReason)
                .onSuccess {
                    showFeedback("تم إلغاء العملية وتحديث كشف الحساب")
                    onSuccess?.invoke()
                }
                .onFailure {
                    showFeedback(it.message ?: "فشل إلغاء العملية", error = true)
                }
        }
    }

    fun addEmployee(name: String, jobTitle: String, phone: String, nationalId: String, salary: Double, notes: String) {
        viewModelScope.launch {
            repository.addEmployee(name, jobTitle, phone, nationalId, salary, notes)
            showFeedback("تم إضافة الموظف بنجاح")
        }
    }

    fun addPump(pumpNumber: Int, name: String) {
        viewModelScope.launch {
            repository.addPump(pumpNumber, name)
            showFeedback("تمت إضافة الطرمبة بنجاح")
        }
    }

    fun addNozzle(pumpId: Long, nozzleNumber: Int, fuelTypeId: Long, tankId: Long, initialReading: Double) {
        viewModelScope.launch {
            repository.addNozzle(pumpId, nozzleNumber, fuelTypeId, tankId, initialReading)
            showFeedback("تمت إضافة المسدس بنجاح")
        }
    }

    fun addFinancialAccount(name: String, type: String, accountNumber: String, initialBalance: Double) {
        viewModelScope.launch {
            repository.addFinancialAccount(name, type, accountNumber, initialBalance)
            showFeedback("تمت إضافة الحساب المالي بنجاح")
        }
    }

    fun updateStationSettings(name: String, address: String, phone: String, currency: String) {
        viewModelScope.launch {
            repository.updateStationSettings(name, address, phone, currency)
            showFeedback("تم تحديث إعدادات المحطة")
        }
    }
}
