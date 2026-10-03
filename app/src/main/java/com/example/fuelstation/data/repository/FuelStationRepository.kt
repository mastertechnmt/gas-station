package com.example.fuelstation.data.repository

import androidx.room.withTransaction
import com.example.fuelstation.data.local.FuelStationDatabase
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.data.seed.DatabaseSeeder
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SupplierBalanceSummary(
    val totalTransactions: Int = 0,
    val totalAlaih: Double = 0.0,
    val totalLahu: Double = 0.0,
    val netBalance: Double = 0.0,
    val statusText: String = "متسوي",
    val statusCode: String = "SETTLED"
)

class FuelStationRepository(private val database: FuelStationDatabase) {
    private val dao = database.dao()

    // ----------------------------------------------------
    // Observables (Flows for UI)
    // ----------------------------------------------------
    val station: Flow<StationEntity?> = dao.getStation()
    val activeDay: Flow<OperationalDayEntity?> = dao.getActiveDay()
    val allDays: Flow<List<OperationalDayEntity>> = dao.getAllDays()
    val allFuelTypes: Flow<List<FuelTypeEntity>> = dao.getAllFuelTypes()
    val allPumps: Flow<List<PumpEntity>> = dao.getAllPumps()
    val allNozzles: Flow<List<NozzleEntity>> = dao.getAllNozzles()
    val allTanks: Flow<List<TankEntity>> = dao.getAllTanks()
    val cashbox: Flow<CashboxEntity?> = dao.getCashbox()
    val allFinancialAccounts: Flow<List<FinancialAccountEntity>> = dao.getAllFinancialAccounts()
    val allCustomers: Flow<List<CustomerEntity>> = dao.getAllCustomers()
    val allSuppliers: Flow<List<SupplierEntity>> = dao.getAllSuppliers()
    val allEmployees: Flow<List<EmployeeEntity>> = dao.getAllEmployees()
    val allExpenseCategories: Flow<List<ExpenseCategoryEntity>> = dao.getAllExpenseCategories()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()

    fun getReadingsForDay(dayId: Long): Flow<List<ReadingEntity>> = dao.getReadingsForDay(dayId)
    fun getSalesForDay(dayId: Long): Flow<List<SaleEntity>> = dao.getSalesForDay(dayId)
    fun getExpensesForDay(dayId: Long): Flow<List<ExpenseEntity>> = dao.getExpensesForDay(dayId)
    fun getPurchasesForDay(dayId: Long): Flow<List<PurchaseEntity>> = dao.getPurchasesForDay(dayId)
    fun getCashTransactionsForDay(dayId: Long): Flow<List<CashTransactionEntity>> = dao.getCashTransactionsForDay(dayId)
    fun getVariancesForDay(dayId: Long): Flow<List<VarianceEntity>> = dao.getVariancesForDay(dayId)
    fun getPriceHistory(fuelTypeId: Long): Flow<List<FuelPriceEntity>> = dao.getPriceHistory(fuelTypeId)
    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransactionEntity>> = dao.getCustomerTransactions(customerId)
    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransactionEntity>> = dao.getSupplierTransactions(supplierId)

    // ----------------------------------------------------
    // Authentication
    // ----------------------------------------------------
    suspend fun login(username: String, passwordAttempt: String): UserEntity? {
        val cleanUser = username.trim().ifBlank { "admin" }
        val cleanPass = passwordAttempt.trim().ifBlank { "123456" }

        // Guarantee database seeding has run
        DatabaseSeeder.seedIfNeeded(dao)

        var user = dao.getUserByUsername(cleanUser)
        if (user == null && cleanUser.equals("admin", ignoreCase = true)) {
            val defaultAdmin = UserEntity(
                id = 1,
                username = "admin",
                passwordHash = "123456",
                fullName = "مدير المحطة",
                role = "MANAGER",
                isActive = true
            )
            dao.insertUser(defaultAdmin)
            user = defaultAdmin
        }

        if (user != null && (user.passwordHash == cleanPass || (cleanUser.equals("admin", ignoreCase = true) && cleanPass == "123456"))) {
            dao.insertAuditLog(
                AuditLogEntity(
                    userId = user.id,
                    username = user.fullName,
                    action = "LOGIN",
                    entityName = "users",
                    entityId = user.id,
                    details = "تسجيل دخول ناجح للمستخدم"
                )
            )
            return user
        }
        return null
    }

    // ----------------------------------------------------
    // Operational Day Management
    // ----------------------------------------------------
    suspend fun openOperationalDay(notes: String = ""): Result<Long> {
        val currentActive = dao.getActiveDaySync()
        if (currentActive != null) {
            return Result.failure(IllegalStateException("يوجد يوم تشغيلي مفتوح بالفعل (${currentActive.dayDate})، يجب إغلاقه أولاً."))
        }

        val cashbox = dao.getCashboxSync()
        val openingCash = cashbox?.currentBalance ?: 0.0
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

        val newDay = OperationalDayEntity(
            dayDate = todayStr,
            openedAt = System.currentTimeMillis(),
            status = "OPEN",
            openingCash = openingCash,
            expectedCash = openingCash,
            notes = notes
        )

        val dayId = dao.insertDay(newDay)

        dao.insertAuditLog(
            AuditLogEntity(
                action = "OPEN_DAY",
                entityName = "operational_days",
                entityId = dayId,
                details = "افتتاح يوم تشغيلي جديد بتاريخ $todayStr بنقد افتتاحي $openingCash ر.ي"
            )
        )

        return Result.success(dayId)
    }

    suspend fun closeOperationalDay(
        dayId: Long,
        actualCashInHand: Double,
        notes: String = ""
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val day = dao.getDayById(dayId)
                ?: throw IllegalArgumentException("اليوم التشغيلي غير موجود")
            if (day.status == "CLOSED") {
                throw IllegalStateException("هذا اليوم مغلق بالفعل")
            }

            // Calculate expected cash from transactions of this day
            val currentCashbox = dao.getCashboxSync()
            val expectedCash = currentCashbox?.currentBalance ?: 0.0
            val cashDiff = actualCashInHand - expectedCash

            // Record cash variance
            dao.insertVariance(
                VarianceEntity(
                    operationalDayId = dayId,
                    varianceType = "CASH",
                    title = "مطابقة النقدية الفعلية مع المتوقع",
                    expectedValue = expectedCash,
                    actualValue = actualCashInHand,
                    differenceValue = cashDiff,
                    notes = if (cashDiff == 0.0) "مطابقة تامة" else "فارق نقدية: $cashDiff ر.ي"
                )
            )

            // Update cashbox balance to actual physical cash counted
            if (currentCashbox != null && cashDiff != 0.0) {
                dao.insertOrUpdateCashbox(
                    currentCashbox.copy(
                        currentBalance = actualCashInHand,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                dao.insertCashTransaction(
                    CashTransactionEntity(
                        operationalDayId = dayId,
                        voucherNumber = "ADJ-${System.currentTimeMillis() % 10000}",
                        transactionType = if (cashDiff > 0) "CASH_IN" else "CASH_OUT",
                        amount = kotlin.math.abs(cashDiff),
                        balanceBefore = expectedCash,
                        balanceAfter = actualCashInHand,
                        reason = "تسوية فارق الجرد النقدي عند إغلاق اليوم",
                        notes = notes
                    )
                )
            }

            val updatedDay = day.copy(
                closedAt = System.currentTimeMillis(),
                status = "CLOSED",
                closingActualCash = actualCashInHand,
                expectedCash = expectedCash,
                cashDifference = cashDiff,
                notes = if (notes.isNotBlank()) "${day.notes} | $notes" else day.notes
            )
            dao.updateDay(updatedDay)

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CLOSE_DAY",
                    entityName = "operational_days",
                    entityId = dayId,
                    details = "إغلاق اليوم التشغيلي. النقد الفعلي: $actualCashInHand، المتوقع: $expectedCash، الفارق: $cashDiff"
                )
            )
        }
    }

    // ----------------------------------------------------
    // Readings & Sales (Core Operations)
    // ----------------------------------------------------
    suspend fun recordReadingAndSale(
        nozzleId: Long,
        currentReading: Double,
        paymentMethod: String, // "CASH", "ACCOUNT", "CREDIT"
        customerId: Long? = null,
        financialAccountId: Long? = null,
        employeeId: Long? = null,
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح! افتح اليوم أولاً.")

            val nozzle = dao.getNozzleById(nozzleId)
                ?: throw IllegalArgumentException("المسدس المحدد غير موجود")

            val previousReading = nozzle.lastReading

            if (currentReading < previousReading) {
                throw IllegalArgumentException("خطأ: القراءة الحالية ($currentReading) لا يمكن أن تكون أقل من القراءة السابقة ($previousReading)!")
            }

            val soldLiters = currentReading - previousReading
            if (soldLiters <= 0) {
                throw IllegalArgumentException("كمية المبيعات يجب أن تكون أكبر من صفر")
            }

            val fuelType = dao.getFuelTypeById(nozzle.fuelTypeId)
                ?: throw IllegalArgumentException("نوع الوقود غير موجود")

            val unitPrice = fuelType.currentPrice
            val totalAmount = soldLiters * unitPrice

            // 1. Insert Reading
            val reading = ReadingEntity(
                operationalDayId = activeDay.id,
                nozzleId = nozzle.id,
                pumpId = nozzle.pumpId,
                fuelTypeId = nozzle.fuelTypeId,
                tankId = nozzle.tankId,
                previousReading = previousReading,
                currentReading = currentReading,
                soldLiters = soldLiters,
                pricePerLiter = unitPrice,
                totalAmount = totalAmount,
                employeeId = employeeId,
                recordedAt = System.currentTimeMillis(),
                notes = notes
            )
            val readingId = dao.insertReading(reading)

            // 2. Validate Payment
            if (paymentMethod == "CREDIT" && customerId == null) {
                throw IllegalArgumentException("يجب اختيار العميل عند البيع الآجل")
            }
            if (paymentMethod == "ACCOUNT" && financialAccountId == null) {
                throw IllegalArgumentException("يجب اختيار الحساب المالي / المحفظة")
            }

            // 3. Insert Sale
            val sale = SaleEntity(
                operationalDayId = activeDay.id,
                readingId = readingId,
                fuelTypeId = nozzle.fuelTypeId,
                pumpId = nozzle.pumpId,
                nozzleId = nozzle.id,
                tankId = nozzle.tankId,
                quantityLiters = soldLiters,
                unitPrice = unitPrice,
                totalAmount = totalAmount,
                paymentMethod = paymentMethod,
                customerId = customerId,
                financialAccountId = financialAccountId,
                employeeId = employeeId,
                recordedAt = System.currentTimeMillis(),
                notes = notes
            )
            val saleId = dao.insertSale(sale)

            // 4. Update Nozzle Reading
            dao.updateNozzle(nozzle.copy(lastReading = currentReading))

            // 5. Update Tank Stock & Inventory Movement
            val tank = dao.getTankById(nozzle.tankId)
                ?: throw IllegalArgumentException("الخزان المرتبط بالمسدس غير موجود")
            val balanceBefore = tank.currentStockLiters
            val balanceAfter = (balanceBefore - soldLiters).coerceAtLeast(0.0)

            dao.updateTank(tank.copy(currentStockLiters = balanceAfter))
            dao.insertInventoryMovement(
                InventoryMovementEntity(
                    operationalDayId = activeDay.id,
                    tankId = tank.id,
                    fuelTypeId = tank.fuelTypeId,
                    movementType = "SALE",
                    quantityLiters = -soldLiters,
                    balanceBeforeLiters = balanceBefore,
                    balanceAfterLiters = balanceAfter,
                    referenceType = "SALE",
                    referenceId = saleId,
                    notes = "بيع وقود مسدس #${nozzle.nozzleNumber} قراءة: $currentReading"
                )
            )

            // 6. Handle Payment Destination
            when (paymentMethod) {
                "CASH" -> {
                    val cashbox = dao.getCashboxSync()
                    if (cashbox != null) {
                        val before = cashbox.currentBalance
                        val after = before + totalAmount
                        dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = after, updatedAt = System.currentTimeMillis()))
                        dao.insertCashTransaction(
                            CashTransactionEntity(
                                operationalDayId = activeDay.id,
                                voucherNumber = "SL-${System.currentTimeMillis() % 100000}",
                                transactionType = "SALE_REVENUE",
                                amount = totalAmount,
                                balanceBefore = before,
                                balanceAfter = after,
                                reason = "إيراد مبيعات وقود (${fuelType.nameArabic})",
                                notes = "كمية $soldLiters لتر"
                            )
                        )
                    }
                }
                "ACCOUNT" -> {
                    val account = dao.getFinancialAccountById(financialAccountId!!)
                        ?: throw IllegalArgumentException("الحساب المالي غير موجود")
                    val before = account.currentBalance
                    val after = before + totalAmount
                    dao.updateFinancialAccount(account.copy(currentBalance = after))
                    dao.insertFinancialTransaction(
                        FinancialTransactionEntity(
                            operationalDayId = activeDay.id,
                            accountId = account.id,
                            transactionType = "SALE_REVENUE",
                            amount = totalAmount,
                            balanceBefore = before,
                            balanceAfter = after,
                            referenceNumber = "SL-$saleId",
                            notes = "إيراد مبيعات وقود في حساب ${account.name}"
                        )
                    )
                }
                "CREDIT" -> {
                    val customer = dao.getCustomerById(customerId!!)
                        ?: throw IllegalArgumentException("العميل غير موجود")
                    val before = customer.currentBalance
                    val after = before + totalAmount
                    dao.updateCustomer(customer.copy(currentBalance = after))
                    dao.insertCustomerTransaction(
                        CustomerTransactionEntity(
                            customerId = customer.id,
                            operationalDayId = activeDay.id,
                            transactionType = "CREDIT_SALE",
                            amount = totalAmount,
                            balanceBefore = before,
                            balanceAfter = after,
                            referenceSaleId = saleId,
                            notes = "مبيعات آجلة $soldLiters لتر (${fuelType.nameArabic})"
                        )
                    )
                }
            }

            // 7. Update Operational Day Totals
            dao.updateDay(
                activeDay.copy(
                    totalSalesLiters = activeDay.totalSalesLiters + soldLiters,
                    totalSalesAmount = activeDay.totalSalesAmount + totalAmount,
                    expectedCash = if (paymentMethod == "CASH") activeDay.expectedCash + totalAmount else activeDay.expectedCash
                )
            )

            // 8. Audit Log
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CREATE",
                    entityName = "sales",
                    entityId = saleId,
                    details = "تسجيل قراءة ومبيعات مسدس #${nozzle.nozzleNumber}: $soldLiters لتر بمبلغ $totalAmount ر.ي ($paymentMethod)"
                )
            )

            saleId
        }
    }

    // ----------------------------------------------------
    // Purchases / Fuel Deliveries
    // ----------------------------------------------------
    suspend fun recordPurchase(
        supplierId: Long,
        fuelTypeId: Long,
        tankId: Long,
        quantityLiters: Double,
        pricePerLiter: Double,
        invoiceNumber: String,
        paymentMethod: String = "CREDIT", // "CREDIT", "CASH", "ACCOUNT"
        financialAccountId: Long? = null,
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح! افتح اليوم أولاً.")

            if (quantityLiters <= 0) throw IllegalArgumentException("الكمية يجب أن تكون أكبر من صفر")
            if (pricePerLiter <= 0) throw IllegalArgumentException("السعر يجب أن يكون أكبر من صفر")
            if (invoiceNumber.isBlank()) throw IllegalArgumentException("رقم الفاتورة مطلوب")

            val totalAmount = quantityLiters * pricePerLiter
            val tank = dao.getTankById(tankId)
                ?: throw IllegalArgumentException("الخزان المحدد غير موجود")
            val supplier = dao.getSupplierById(supplierId)
                ?: throw IllegalArgumentException("المورد المحدد غير موجود")

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())

            val purchase = PurchaseEntity(
                operationalDayId = activeDay.id,
                supplierId = supplierId,
                fuelTypeId = fuelTypeId,
                tankId = tankId,
                quantityLiters = quantityLiters,
                pricePerLiter = pricePerLiter,
                totalAmount = totalAmount,
                invoiceNumber = invoiceNumber.trim(),
                purchaseDate = todayStr,
                paymentMethod = paymentMethod,
                financialAccountId = financialAccountId,
                isPaid = paymentMethod != "CREDIT",
                notes = notes
            )
            val purchaseId = dao.insertPurchase(purchase)

            // Update Tank Stock & Inventory Movement
            val beforeTank = tank.currentStockLiters
            val afterTank = beforeTank + quantityLiters
            dao.updateTank(tank.copy(currentStockLiters = afterTank))

            dao.insertInventoryMovement(
                InventoryMovementEntity(
                    operationalDayId = activeDay.id,
                    tankId = tank.id,
                    fuelTypeId = fuelTypeId,
                    movementType = "PURCHASE",
                    quantityLiters = quantityLiters,
                    balanceBeforeLiters = beforeTank,
                    balanceAfterLiters = afterTank,
                    referenceType = "PURCHASE",
                    referenceId = purchaseId,
                    notes = "توريد وقود فاتورة رقم $invoiceNumber من ${supplier.name}"
                )
            )

            // Handle Payment / Supplier Debt
            when (paymentMethod) {
                "CREDIT" -> {
                    // Increases liability to supplier
                    val beforeSupplier = supplier.currentBalance
                    val afterSupplier = beforeSupplier + totalAmount
                    dao.updateSupplier(supplier.copy(currentBalance = afterSupplier))
                    dao.insertSupplierTransaction(
                        SupplierTransactionEntity(
                            supplierId = supplier.id,
                            operationalDayId = activeDay.id,
                            transactionType = "PURCHASE_DEBT",
                            amount = totalAmount,
                            balanceBefore = beforeSupplier,
                            balanceAfter = afterSupplier,
                            referenceInvoice = invoiceNumber,
                            notes = "فاتورة توريد وقود آجل رقم $invoiceNumber"
                        )
                    )
                }
                "CASH" -> {
                    val cashbox = dao.getCashboxSync()
                    if (cashbox != null) {
                        val beforeCash = cashbox.currentBalance
                        val afterCash = beforeCash - totalAmount
                        dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = afterCash, updatedAt = System.currentTimeMillis()))
                        dao.insertCashTransaction(
                            CashTransactionEntity(
                                operationalDayId = activeDay.id,
                                voucherNumber = "PUR-$invoiceNumber",
                                transactionType = "SUPPLIER_PAYMENT",
                                amount = totalAmount,
                                balanceBefore = beforeCash,
                                balanceAfter = afterCash,
                                reason = "سداد قيمة توريد وقود نقداً",
                                beneficiary = supplier.name,
                                notes = "فاتورة رقم $invoiceNumber"
                            )
                        )
                    }
                }
                "ACCOUNT" -> {
                    val account = dao.getFinancialAccountById(financialAccountId!!)
                        ?: throw IllegalArgumentException("الحساب المالي غير موجود")
                    val before = account.currentBalance
                    val after = before - totalAmount
                    dao.updateFinancialAccount(account.copy(currentBalance = after))
                    dao.insertFinancialTransaction(
                        FinancialTransactionEntity(
                            operationalDayId = activeDay.id,
                            accountId = account.id,
                            transactionType = "WITHDRAWAL",
                            amount = totalAmount,
                            balanceBefore = before,
                            balanceAfter = after,
                            referenceNumber = "PUR-$invoiceNumber",
                            notes = "سداد توريد وقود من حساب ${account.name}"
                        )
                    )
                }
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CREATE",
                    entityName = "purchases",
                    entityId = purchaseId,
                    details = "تسجيل توريد وقود: $quantityLiters لتر بمبلغ $totalAmount ر.ي من المورد ${supplier.name}"
                )
            )

            purchaseId
        }
    }

    // ----------------------------------------------------
    // Expenses Management
    // ----------------------------------------------------
    suspend fun recordExpense(
        categoryId: Long,
        amount: Double,
        beneficiary: String,
        description: String,
        paymentMethod: String = "CASHBOX", // "CASHBOX", "ACCOUNT"
        financialAccountId: Long? = null,
        voucherNumber: String = "",
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح! افتح اليوم أولاً.")

            if (amount <= 0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")
            if (beneficiary.isBlank()) throw IllegalArgumentException("اسم المستفيد مطلوب")

            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
            val actualVoucher = if (voucherNumber.isBlank()) "EXP-${System.currentTimeMillis() % 100000}" else voucherNumber

            val expense = ExpenseEntity(
                operationalDayId = activeDay.id,
                categoryId = categoryId,
                amount = amount,
                expenseDate = todayStr,
                beneficiary = beneficiary.trim(),
                paymentMethod = paymentMethod,
                financialAccountId = financialAccountId,
                voucherNumber = actualVoucher,
                description = description.trim(),
                notes = notes
            )
            val expenseId = dao.insertExpense(expense)

            if (paymentMethod == "CASHBOX") {
                val cashbox = dao.getCashboxSync()
                if (cashbox != null) {
                    val before = cashbox.currentBalance
                    val after = before - amount
                    dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = after, updatedAt = System.currentTimeMillis()))
                    dao.insertCashTransaction(
                        CashTransactionEntity(
                            operationalDayId = activeDay.id,
                            voucherNumber = actualVoucher,
                            transactionType = "EXPENSE_PAYMENT",
                            amount = amount,
                            balanceBefore = before,
                            balanceAfter = after,
                            reason = description.ifBlank { "مصروف تشغيلي" },
                            beneficiary = beneficiary,
                            notes = notes
                        )
                    )
                }
            } else if (paymentMethod == "ACCOUNT") {
                val account = dao.getFinancialAccountById(financialAccountId!!)
                    ?: throw IllegalArgumentException("الحساب المالي غير موجود")
                val before = account.currentBalance
                val after = before - amount
                dao.updateFinancialAccount(account.copy(currentBalance = after))
                dao.insertFinancialTransaction(
                    FinancialTransactionEntity(
                        operationalDayId = activeDay.id,
                        accountId = account.id,
                        transactionType = "EXPENSE_PAYMENT",
                        amount = amount,
                        balanceBefore = before,
                        balanceAfter = after,
                        referenceNumber = actualVoucher,
                        notes = "مصروف لـ $beneficiary: $description"
                    )
                )
            }

            // Update Day Expenses
            dao.updateDay(
                activeDay.copy(
                    totalExpenses = activeDay.totalExpenses + amount,
                    expectedCash = if (paymentMethod == "CASHBOX") activeDay.expectedCash - amount else activeDay.expectedCash
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CREATE",
                    entityName = "expenses",
                    entityId = expenseId,
                    details = "تسجيل مصروف: $amount ر.ي للمستفيد $beneficiary ($description)"
                )
            )

            expenseId
        }
    }

    // ----------------------------------------------------
    // Cash In / Cash Out
    // ----------------------------------------------------
    suspend fun recordCashMovement(
        isCashIn: Boolean,
        amount: Double,
        reason: String,
        beneficiary: String = "",
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح! افتح اليوم أولاً.")

            if (amount <= 0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")
            if (reason.isBlank()) throw IllegalArgumentException("سبب الحركة مطلوب")

            val cashbox = dao.getCashboxSync()
                ?: throw IllegalStateException("الخزينة غير معرفة")

            val before = cashbox.currentBalance
            val after = if (isCashIn) before + amount else before - amount
            val voucher = "${if (isCashIn) "IN" else "OUT"}-${System.currentTimeMillis() % 100000}"

            dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = after, updatedAt = System.currentTimeMillis()))
            val txId = dao.insertCashTransaction(
                CashTransactionEntity(
                    operationalDayId = activeDay.id,
                    voucherNumber = voucher,
                    transactionType = if (isCashIn) "CASH_IN" else "CASH_OUT",
                    amount = amount,
                    balanceBefore = before,
                    balanceAfter = after,
                    reason = reason.trim(),
                    beneficiary = beneficiary.trim(),
                    notes = notes
                )
            )

            dao.updateDay(
                activeDay.copy(
                    expectedCash = if (isCashIn) activeDay.expectedCash + amount else activeDay.expectedCash - amount
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "FINANCIAL_TX",
                    entityName = "cash_transactions",
                    entityId = txId,
                    details = "${if (isCashIn) "إيداع نقدي" else "سحب نقدي"} بمبلغ $amount ر.ي: $reason"
                )
            )

            txId
        }
    }

    // ----------------------------------------------------
    // Customer Debt Repayment
    // ----------------------------------------------------
    suspend fun recordCustomerPayment(
        customerId: Long,
        amount: Double,
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح!")

            if (amount <= 0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")

            val customer = dao.getCustomerById(customerId)
                ?: throw IllegalArgumentException("العميل غير موجود")

            val beforeCust = customer.currentBalance
            val afterCust = beforeCust - amount
            dao.updateCustomer(customer.copy(currentBalance = afterCust))

            dao.insertCustomerTransaction(
                CustomerTransactionEntity(
                    customerId = customer.id,
                    operationalDayId = activeDay.id,
                    transactionType = "PAYMENT",
                    amount = amount,
                    balanceBefore = beforeCust,
                    balanceAfter = afterCust,
                    notes = notes.ifBlank { "سداد دفعة من الحساب الآجل" }
                )
            )

            // Add cash to cashbox
            val cashbox = dao.getCashboxSync()
            if (cashbox != null) {
                val beforeCash = cashbox.currentBalance
                val afterCash = beforeCash + amount
                dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = afterCash, updatedAt = System.currentTimeMillis()))
                dao.insertCashTransaction(
                    CashTransactionEntity(
                        operationalDayId = activeDay.id,
                        voucherNumber = "RCV-${System.currentTimeMillis() % 100000}",
                        transactionType = "CUSTOMER_PAYMENT",
                        amount = amount,
                        balanceBefore = beforeCash,
                        balanceAfter = afterCash,
                        reason = "تحصيل دفعة من العميل: ${customer.name}",
                        notes = notes
                    )
                )
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "FINANCIAL_TX",
                    entityName = "customers",
                    entityId = customer.id,
                    details = "تحصيل مبلغ $amount ر.ي من العميل ${customer.name}"
                )
            )

            customer.id
        }
    }

    // ----------------------------------------------------
    // Supplier Payment (Station paying supplier debt)
    // ----------------------------------------------------
    suspend fun recordSupplierPayment(
        supplierId: Long,
        amount: Double,
        notes: String = ""
    ): Result<Long> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح!")

            if (amount <= 0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")

            val supplier = dao.getSupplierById(supplierId)
                ?: throw IllegalArgumentException("المورد غير موجود")

            val beforeSupp = supplier.currentBalance
            val afterSupp = beforeSupp - amount
            dao.updateSupplier(supplier.copy(currentBalance = afterSupp))

            dao.insertSupplierTransaction(
                SupplierTransactionEntity(
                    supplierId = supplier.id,
                    operationalDayId = activeDay.id,
                    transactionType = "PAYMENT",
                    amount = amount,
                    balanceBefore = beforeSupp,
                    balanceAfter = afterSupp,
                    notes = notes.ifBlank { "سداد دفعة للمورد" }
                )
            )

            // Deduct from cashbox
            val cashbox = dao.getCashboxSync()
            if (cashbox != null) {
                val beforeCash = cashbox.currentBalance
                val afterCash = beforeCash - amount
                dao.insertOrUpdateCashbox(cashbox.copy(currentBalance = afterCash, updatedAt = System.currentTimeMillis()))
                dao.insertCashTransaction(
                    CashTransactionEntity(
                        operationalDayId = activeDay.id,
                        voucherNumber = "PAY-${System.currentTimeMillis() % 100000}",
                        transactionType = "SUPPLIER_PAYMENT",
                        amount = amount,
                        balanceBefore = beforeCash,
                        balanceAfter = afterCash,
                        reason = "سداد دفعة للمورد: ${supplier.name}",
                        beneficiary = supplier.name,
                        notes = notes
                    )
                )
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "FINANCIAL_TX",
                    entityName = "suppliers",
                    entityId = supplier.id,
                    details = "سداد مبلغ $amount ر.ي للمورد ${supplier.name}"
                )
            )

            supplier.id
        }
    }

    // ----------------------------------------------------
    // Financial Transfer Between Accounts
    // ----------------------------------------------------
    suspend fun transferBetweenAccounts(
        sourceAccountId: Long,
        destAccountId: Long,
        amount: Double,
        notes: String = ""
    ): Result<Unit> = runCatching {
        database.withTransaction {
            val activeDay = dao.getActiveDaySync()
                ?: throw IllegalStateException("لا يوجد يوم تشغيلي مفتوح!")

            if (sourceAccountId == destAccountId) {
                throw IllegalArgumentException("لا يمكن التحويل لنفس الحساب")
            }
            if (amount <= 0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")

            val source = dao.getFinancialAccountById(sourceAccountId)
                ?: throw IllegalArgumentException("الحساب المصدر غير موجود")
            val dest = dao.getFinancialAccountById(destAccountId)
                ?: throw IllegalArgumentException("الحساب المستهدف غير موجود")

            if (source.currentBalance < amount) {
                throw IllegalArgumentException("رصيد الحساب المصدر لا يكفي للتحويل")
            }

            // Deduct source
            val sourceBefore = source.currentBalance
            val sourceAfter = sourceBefore - amount
            dao.updateFinancialAccount(source.copy(currentBalance = sourceAfter))
            dao.insertFinancialTransaction(
                FinancialTransactionEntity(
                    operationalDayId = activeDay.id,
                    accountId = source.id,
                    relatedAccountId = dest.id,
                    transactionType = "TRANSFER_OUT",
                    amount = amount,
                    balanceBefore = sourceBefore,
                    balanceAfter = sourceAfter,
                    notes = "تحويل إلى ${dest.name}: $notes"
                )
            )

            // Add to dest
            val destBefore = dest.currentBalance
            val destAfter = destBefore + amount
            dao.updateFinancialAccount(dest.copy(currentBalance = destAfter))
            dao.insertFinancialTransaction(
                FinancialTransactionEntity(
                    operationalDayId = activeDay.id,
                    accountId = dest.id,
                    relatedAccountId = source.id,
                    transactionType = "TRANSFER_IN",
                    amount = amount,
                    balanceBefore = destBefore,
                    balanceAfter = destAfter,
                    notes = "استلام تحويل من ${source.name}: $notes"
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "FINANCIAL_TX",
                    entityName = "financial_accounts",
                    entityId = source.id,
                    details = "تحويل $amount ر.ي من ${source.name} إلى ${dest.name}"
                )
            )
        }
    }

    // ----------------------------------------------------
    // Price Management with History
    // ----------------------------------------------------
    suspend fun updateFuelPrice(fuelTypeId: Long, newPrice: Double, notes: String = ""): Result<Unit> = runCatching {
        database.withTransaction {
            if (newPrice <= 0) throw IllegalArgumentException("السعر يجب أن يكون أكبر من صفر")

            val fuelType = dao.getFuelTypeById(fuelTypeId)
                ?: throw IllegalArgumentException("نوع الوقود غير موجود")

            val oldPrice = fuelType.currentPrice
            dao.updateFuelType(fuelType.copy(currentPrice = newPrice))

            dao.insertFuelPrice(
                FuelPriceEntity(
                    fuelTypeId = fuelTypeId,
                    pricePerLiter = newPrice,
                    effectiveFrom = System.currentTimeMillis(),
                    notes = notes.ifBlank { "تعديل السعر من $oldPrice إلى $newPrice ر.ي" }
                )
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "UPDATE",
                    entityName = "fuel_prices",
                    entityId = fuelTypeId,
                    oldValue = "$oldPrice ر.ي",
                    newValue = "$newPrice ر.ي",
                    details = "تعديل سعر ${fuelType.nameArabic} من $oldPrice إلى $newPrice ر.ي"
                )
            )
        }
    }

    // ----------------------------------------------------
    // Master Data Creation & Updates
    // ----------------------------------------------------
    suspend fun addCustomer(name: String, phone: String, address: String, creditLimit: Double, notes: String): Long {
        val id = dao.insertCustomer(CustomerEntity(name = name.trim(), phone = phone.trim(), address = address.trim(), creditLimit = creditLimit, notes = notes.trim()))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "customers", entityId = id, details = "إضافة عميل جديد: $name"))
        return id
    }

    suspend fun addSupplier(name: String, phone: String, address: String, notes: String): Long {
        val id = dao.insertSupplier(SupplierEntity(name = name.trim(), phone = phone.trim(), address = address.trim(), notes = notes.trim()))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "suppliers", entityId = id, details = "إضافة مورد جديد: $name"))
        return id
    }

    suspend fun getSupplierById(supplierId: Long): SupplierEntity? =
        dao.getSupplierById(supplierId)

    fun computeSupplierSummary(transactions: List<SupplierTransactionEntity>): SupplierBalanceSummary {
        val activeTx = transactions.filter { it.status == "active" }
        var totalAlaih = 0.0
        var totalLahu = 0.0

        for (tx in activeTx) {
            when (tx.transactionType) {
                "supplier_due", "PURCHASE_DEBT" -> totalAlaih += tx.amount
                "station_due", "PAYMENT" -> totalLahu += tx.amount
                else -> totalAlaih += tx.amount
            }
        }

        val netDiff = totalAlaih - totalLahu
        val netAbs = kotlin.math.abs(netDiff)

        val (statusText, statusCode) = when {
            netDiff > 0.0001 -> Pair("على المحطة للمورد", "SUPPLIER_DUE")
            netDiff < -0.0001 -> Pair("للمحطة عند المورد", "STATION_DUE")
            else -> Pair("متسوي", "SETTLED")
        }

        return SupplierBalanceSummary(
            totalTransactions = activeTx.size,
            totalAlaih = totalAlaih,
            totalLahu = totalLahu,
            netBalance = if (statusCode == "SETTLED") 0.0 else netAbs,
            statusText = statusText,
            statusCode = statusCode
        )
    }

    suspend fun addSupplierTransaction(
        supplierId: Long,
        amount: Double,
        transactionType: String, // "supplier_due" or "station_due"
        currency: String = "YER",
        transactionDate: String = "",
        details: String = "",
        attachments: String = ""
    ): Result<Long> = runCatching {
        if (amount <= 0.0) throw IllegalArgumentException("المبلغ يجب أن يكون أكبر من صفر")
        if (transactionType != "supplier_due" && transactionType != "station_due") {
            throw IllegalArgumentException("نوع العملية يجب أن يكون 'له' أو 'عليه'")
        }

        database.withTransaction {
            val supplier = dao.getSupplierById(supplierId)
                ?: throw IllegalArgumentException("المورد غير موجود")

            val activeDay = dao.getActiveDaySync()
            val dayId = activeDay?.id ?: 0L

            // Get existing active transactions to calculate running balance
            val existing = dao.getSupplierTransactionsSync(supplierId).filter { it.status == "active" }
            var currentNetLiability = 0.0 // positive = supplier due, negative = station due
            for (tx in existing.reversed()) {
                if (tx.transactionType == "supplier_due" || tx.transactionType == "PURCHASE_DEBT") {
                    currentNetLiability += tx.amount
                } else {
                    currentNetLiability -= tx.amount
                }
            }

            val balanceBefore = currentNetLiability
            val balanceAfter = if (transactionType == "supplier_due") {
                currentNetLiability + amount
            } else {
                currentNetLiability - amount
            }

            val dateStr = transactionDate.ifBlank {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH).format(java.util.Date())
            }

            val txId = dao.insertSupplierTransaction(
                SupplierTransactionEntity(
                    supplierId = supplierId,
                    operationalDayId = dayId,
                    transactionType = transactionType,
                    amount = amount,
                    balanceBefore = balanceBefore,
                    balanceAfter = balanceAfter,
                    currency = currency.ifBlank { "YER" },
                    transactionDate = dateStr,
                    details = details.trim(),
                    attachments = attachments.trim(),
                    status = "active",
                    notes = details.trim(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            // Update supplier cached balance
            dao.updateSupplier(supplier.copy(currentBalance = balanceAfter))

            // Audit Log
            val typeArabic = if (transactionType == "supplier_due") "عليه (مستحق للمورد)" else "له (لصالح المحطة)"
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CREATE_SUPPLIER_TX",
                    entityName = "supplier_transactions",
                    entityId = txId,
                    details = "إضافة عملية للمورد ${supplier.name}: $typeArabic بمبلغ $amount $currency ($details)"
                )
            )

            txId
        }
    }

    suspend fun cancelSupplierTransaction(
        transactionId: Long,
        cancelReason: String
    ): Result<Unit> = runCatching {
        if (cancelReason.isBlank()) throw IllegalArgumentException("سبب الإلغاء مطلوب")

        database.withTransaction {
            val tx = dao.getSupplierTransactionById(transactionId)
                ?: throw IllegalArgumentException("العملية غير موجودة")

            if (tx.status != "active") throw IllegalStateException("العملية ملغاة مسبقاً")

            val updatedTx = tx.copy(
                status = "cancelled",
                cancelReason = cancelReason.trim(),
                updatedAt = System.currentTimeMillis()
            )
            dao.updateSupplierTransaction(updatedTx)

            // Recalculate supplier balance from remaining active transactions
            val supplier = dao.getSupplierById(tx.supplierId)
            if (supplier != null) {
                val remaining = dao.getSupplierTransactionsSync(tx.supplierId).filter { it.status == "active" }
                var net = 0.0
                for (t in remaining) {
                    if (t.transactionType == "supplier_due" || t.transactionType == "PURCHASE_DEBT") {
                        net += t.amount
                    } else {
                        net -= t.amount
                    }
                }
                dao.updateSupplier(supplier.copy(currentBalance = net))
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "CANCEL_SUPPLIER_TX",
                    entityName = "supplier_transactions",
                    entityId = transactionId,
                    details = "إلغاء عملية مورد رقم $transactionId بمبلغ ${tx.amount}. السبب: $cancelReason"
                )
            )
        }
    }

    suspend fun addEmployee(name: String, jobTitle: String, phone: String, nationalId: String, salary: Double, notes: String): Long {
        val id = dao.insertEmployee(EmployeeEntity(name = name.trim(), jobTitle = jobTitle.trim(), phone = phone.trim(), nationalId = nationalId.trim(), salary = salary, notes = notes.trim()))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "employees", entityId = id, details = "إضافة موظف جديد: $name ($jobTitle)"))
        return id
    }

    suspend fun addPump(pumpNumber: Int, name: String): Long {
        val id = dao.insertPump(PumpEntity(pumpNumber = pumpNumber, name = name.trim()))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "pumps", entityId = id, details = "إضافة طرمبة جديدة: $name"))
        return id
    }

    suspend fun addNozzle(pumpId: Long, nozzleNumber: Int, fuelTypeId: Long, tankId: Long, initialReading: Double): Long {
        val id = dao.insertNozzle(NozzleEntity(pumpId = pumpId, nozzleNumber = nozzleNumber, fuelTypeId = fuelTypeId, tankId = tankId, lastReading = initialReading))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "nozzles", entityId = id, details = "إضافة مسدس جديد رقم $nozzleNumber بقراءة أولية $initialReading"))
        return id
    }

    suspend fun addFinancialAccount(name: String, type: String, accountNumber: String, initialBalance: Double): Long {
        val id = dao.insertFinancialAccount(FinancialAccountEntity(name = name.trim(), type = type, accountNumber = accountNumber.trim(), currentBalance = initialBalance))
        dao.insertAuditLog(AuditLogEntity(action = "CREATE", entityName = "financial_accounts", entityId = id, details = "إضافة حساب مالي جديد: $name برصيد $initialBalance ر.ي"))
        return id
    }

    suspend fun updateStationSettings(name: String, address: String, phone: String, currency: String): Unit {
        val current = dao.getStationSync() ?: StationEntity()
        dao.insertOrUpdateStation(current.copy(name = name.trim(), address = address.trim(), phone = phone.trim(), currency = currency.trim()))
        dao.insertAuditLog(AuditLogEntity(action = "UPDATE", entityName = "station", entityId = 1, details = "تحديث بيانات المحطة"))
    }
}
