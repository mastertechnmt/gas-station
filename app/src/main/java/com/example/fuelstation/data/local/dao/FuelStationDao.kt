package com.example.fuelstation.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.fuelstation.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelStationDao {

    // Station & Settings
    @Query("SELECT * FROM station WHERE id = 1 LIMIT 1")
    fun getStation(): Flow<StationEntity?>

    @Query("SELECT * FROM station WHERE id = 1 LIMIT 1")
    suspend fun getStationSync(): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStation(station: StationEntity)

    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    fun getSetting(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSettingEntity)

    // Users
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    // Operational Day
    @Query("SELECT * FROM operational_days WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1")
    fun getActiveDay(): Flow<OperationalDayEntity?>

    @Query("SELECT * FROM operational_days WHERE status = 'OPEN' ORDER BY id DESC LIMIT 1")
    suspend fun getActiveDaySync(): OperationalDayEntity?

    @Query("SELECT * FROM operational_days WHERE id = :id LIMIT 1")
    suspend fun getDayById(id: Long): OperationalDayEntity?

    @Query("SELECT * FROM operational_days ORDER BY id DESC")
    fun getAllDays(): Flow<List<OperationalDayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(day: OperationalDayEntity): Long

    @Update
    suspend fun updateDay(day: OperationalDayEntity)

    // Fuel Types & Prices
    @Query("SELECT * FROM fuel_types ORDER BY id ASC")
    fun getAllFuelTypes(): Flow<List<FuelTypeEntity>>

    @Query("SELECT * FROM fuel_types ORDER BY id ASC")
    suspend fun getAllFuelTypesSync(): List<FuelTypeEntity>

    @Query("SELECT * FROM fuel_types WHERE id = :id LIMIT 1")
    suspend fun getFuelTypeById(id: Long): FuelTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelType(fuelType: FuelTypeEntity): Long

    @Update
    suspend fun updateFuelType(fuelType: FuelTypeEntity)

    @Query("SELECT * FROM fuel_prices WHERE fuelTypeId = :fuelTypeId ORDER BY effectiveFrom DESC")
    fun getPriceHistory(fuelTypeId: Long): Flow<List<FuelPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelPrice(price: FuelPriceEntity): Long

    // Pumps & Nozzles
    @Query("SELECT * FROM pumps ORDER BY pumpNumber ASC")
    fun getAllPumps(): Flow<List<PumpEntity>>

    @Query("SELECT * FROM pumps ORDER BY pumpNumber ASC")
    suspend fun getAllPumpsSync(): List<PumpEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPump(pump: PumpEntity): Long

    @Update
    suspend fun updatePump(pump: PumpEntity)

    @Delete
    suspend fun deletePump(pump: PumpEntity)

    @Query("SELECT * FROM nozzles ORDER BY pumpId ASC, nozzleNumber ASC")
    fun getAllNozzles(): Flow<List<NozzleEntity>>

    @Query("SELECT * FROM nozzles ORDER BY pumpId ASC, nozzleNumber ASC")
    suspend fun getAllNozzlesSync(): List<NozzleEntity>

    @Query("SELECT * FROM nozzles WHERE id = :id LIMIT 1")
    suspend fun getNozzleById(id: Long): NozzleEntity?

    @Query("SELECT * FROM nozzles WHERE pumpId = :pumpId ORDER BY nozzleNumber ASC")
    fun getNozzlesForPump(pumpId: Long): Flow<List<NozzleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNozzle(nozzle: NozzleEntity): Long

    @Update
    suspend fun updateNozzle(nozzle: NozzleEntity)

    @Delete
    suspend fun deleteNozzle(nozzle: NozzleEntity)

    // Tanks
    @Query("SELECT * FROM tanks ORDER BY tankNumber ASC")
    fun getAllTanks(): Flow<List<TankEntity>>

    @Query("SELECT * FROM tanks ORDER BY tankNumber ASC")
    suspend fun getAllTanksSync(): List<TankEntity>

    @Query("SELECT * FROM tanks WHERE id = :id LIMIT 1")
    suspend fun getTankById(id: Long): TankEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTank(tank: TankEntity): Long

    @Update
    suspend fun updateTank(tank: TankEntity)

    // Readings & Sales
    @Query("SELECT * FROM readings WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getReadingsForDay(dayId: Long): Flow<List<ReadingEntity>>

    @Query("SELECT * FROM readings ORDER BY id DESC")
    fun getAllReadings(): Flow<List<ReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: ReadingEntity): Long

    @Query("SELECT * FROM sales WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getSalesForDay(dayId: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales ORDER BY id DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE operationalDayId = :dayId")
    suspend fun getSalesForDaySync(dayId: Long): List<SaleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    // Inventory Movements & Purchases
    @Query("SELECT * FROM inventory_movements WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getInventoryMovementsForDay(dayId: Long): Flow<List<InventoryMovementEntity>>

    @Query("SELECT * FROM inventory_movements ORDER BY id DESC")
    fun getAllInventoryMovements(): Flow<List<InventoryMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryMovement(movement: InventoryMovementEntity): Long

    @Query("SELECT * FROM purchases WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getPurchasesForDay(dayId: Long): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases ORDER BY id DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity): Long

    // Suppliers & Supplier Transactions
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: Long): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity): Long

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY id DESC")
    fun getSupplierTransactions(supplierId: Long): Flow<List<SupplierTransactionEntity>>

    @Query("SELECT * FROM supplier_transactions WHERE supplierId = :supplierId ORDER BY id DESC")
    suspend fun getSupplierTransactionsSync(supplierId: Long): List<SupplierTransactionEntity>

    @Query("SELECT * FROM supplier_transactions WHERE id = :id LIMIT 1")
    suspend fun getSupplierTransactionById(id: Long): SupplierTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplierTransaction(tx: SupplierTransactionEntity): Long

    @Update
    suspend fun updateSupplierTransaction(tx: SupplierTransactionEntity)

    // Customers & Customer Transactions
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY id DESC")
    fun getCustomerTransactions(customerId: Long): Flow<List<CustomerTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTransaction(tx: CustomerTransactionEntity): Long

    // Cashbox & Cash Transactions
    @Query("SELECT * FROM cashbox WHERE id = 1 LIMIT 1")
    fun getCashbox(): Flow<CashboxEntity?>

    @Query("SELECT * FROM cashbox WHERE id = 1 LIMIT 1")
    suspend fun getCashboxSync(): CashboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCashbox(cashbox: CashboxEntity)

    @Query("SELECT * FROM cash_transactions WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getCashTransactionsForDay(dayId: Long): Flow<List<CashTransactionEntity>>

    @Query("SELECT * FROM cash_transactions ORDER BY id DESC")
    fun getAllCashTransactions(): Flow<List<CashTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCashTransaction(tx: CashTransactionEntity): Long

    // Financial Accounts & Transactions
    @Query("SELECT * FROM financial_accounts WHERE isActive = 1 ORDER BY name ASC")
    fun getAllFinancialAccounts(): Flow<List<FinancialAccountEntity>>

    @Query("SELECT * FROM financial_accounts WHERE id = :id LIMIT 1")
    suspend fun getFinancialAccountById(id: Long): FinancialAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialAccount(account: FinancialAccountEntity): Long

    @Update
    suspend fun updateFinancialAccount(account: FinancialAccountEntity)

    @Query("SELECT * FROM financial_transactions WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getFinancialTransactionsForDay(dayId: Long): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM financial_transactions ORDER BY id DESC")
    fun getAllFinancialTransactions(): Flow<List<FinancialTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinancialTransaction(tx: FinancialTransactionEntity): Long

    // Expense Categories & Expenses
    @Query("SELECT * FROM expense_categories ORDER BY id ASC")
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategoryEntity): Long

    @Query("SELECT * FROM expenses WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getExpensesForDay(dayId: Long): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE operationalDayId = :dayId")
    suspend fun getExpensesForDaySync(dayId: Long): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    // Employees
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: Long): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity): Long

    @Update
    suspend fun updateEmployee(employee: EmployeeEntity)

    // Variances
    @Query("SELECT * FROM variances WHERE operationalDayId = :dayId ORDER BY id DESC")
    fun getVariancesForDay(dayId: Long): Flow<List<VarianceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariance(variance: VarianceEntity): Long

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long
}
