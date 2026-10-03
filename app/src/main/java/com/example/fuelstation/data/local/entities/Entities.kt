package com.example.fuelstation.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val role: String = "MANAGER", // MANAGER, ACCOUNTANT, OPERATOR
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "station")
data class StationEntity(
    @PrimaryKey val id: Long = 1,
    val name: String = "محطة الوقود الرئيسية",
    val address: String = "صنعاء - الجمهورية اليمنية",
    val phone: String = "+967 770 000 000",
    val currency: String = "ريال يمني",
    val currencySymbol: String = "ر.ي",
    val timezone: String = "Asia/Aden",
    val openingHour: String = "06:00",
    val closingHour: String = "23:00",
    val logoUrl: String? = null
)

@Entity(tableName = "operational_days")
data class OperationalDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayDate: String, // YYYY-MM-DD
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val status: String = "OPEN", // "OPEN", "CLOSED"
    val openedByUserId: Long = 1,
    val closedByUserId: Long? = null,
    val openingCash: Double = 0.0,
    val closingActualCash: Double? = null,
    val expectedCash: Double = 0.0,
    val cashDifference: Double = 0.0,
    val totalSalesLiters: Double = 0.0,
    val totalSalesAmount: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "fuel_types")
data class FuelTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String, // G91, G95, DSL
    val nameArabic: String, // بنزين 91, بنزين 95, ديزل
    val nameEnglish: String,
    val colorHex: String, // #10B981, #0284C7, #F59E0B
    val currentPrice: Double,
    val isAvailable: Boolean = true
)

@Entity(
    tableName = "fuel_prices",
    foreignKeys = [
        ForeignKey(
            entity = FuelTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fuelTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["fuelTypeId"])]
)
data class FuelPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fuelTypeId: Long,
    val pricePerLiter: Double,
    val effectiveFrom: Long = System.currentTimeMillis(),
    val effectiveTo: Long? = null,
    val notes: String = ""
)

@Entity(tableName = "pumps")
data class PumpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pumpNumber: Int,
    val name: String, // الطرمبة 1
    val status: String = "ACTIVE" // ACTIVE, MAINTENANCE
)

@Entity(
    tableName = "tanks",
    foreignKeys = [
        ForeignKey(
            entity = FuelTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fuelTypeId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["fuelTypeId"])]
)
data class TankEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tankNumber: Int,
    val name: String, // خزان بنزين 91 الرئيسي
    val fuelTypeId: Long,
    val capacityLiters: Double,
    val currentStockLiters: Double,
    val minAlertLiters: Double = 2000.0,
    val criticalAlertLiters: Double = 1000.0
)

@Entity(
    tableName = "nozzles",
    foreignKeys = [
        ForeignKey(
            entity = PumpEntity::class,
            parentColumns = ["id"],
            childColumns = ["pumpId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FuelTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["fuelTypeId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TankEntity::class,
            parentColumns = ["id"],
            childColumns = ["tankId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["pumpId"]),
        Index(value = ["fuelTypeId"]),
        Index(value = ["tankId"])
    ]
)
data class NozzleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pumpId: Long,
    val nozzleNumber: Int, // 1, 2, 3
    val fuelTypeId: Long,
    val tankId: Long,
    val lastReading: Double = 0.0,
    val status: String = "ACTIVE"
)

@Entity(
    tableName = "readings",
    foreignKeys = [
        ForeignKey(
            entity = OperationalDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["operationalDayId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = NozzleEntity::class,
            parentColumns = ["id"],
            childColumns = ["nozzleId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["operationalDayId"]),
        Index(value = ["nozzleId"])
    ]
)
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val nozzleId: Long,
    val pumpId: Long,
    val fuelTypeId: Long,
    val tankId: Long,
    val previousReading: Double,
    val currentReading: Double,
    val soldLiters: Double,
    val pricePerLiter: Double,
    val totalAmount: Double,
    val employeeId: Long? = null,
    val recordedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "sales",
    foreignKeys = [
        ForeignKey(
            entity = OperationalDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["operationalDayId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ReadingEntity::class,
            parentColumns = ["id"],
            childColumns = ["readingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["operationalDayId"]),
        Index(value = ["readingId"])
    ]
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val readingId: Long,
    val fuelTypeId: Long,
    val pumpId: Long,
    val nozzleId: Long,
    val tankId: Long,
    val quantityLiters: Double,
    val unitPrice: Double,
    val totalAmount: Double,
    val paymentMethod: String = "CASH", // CASH, ACCOUNT, CREDIT
    val customerId: Long? = null,
    val financialAccountId: Long? = null,
    val employeeId: Long? = null,
    val recordedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(
    tableName = "inventory_movements",
    foreignKeys = [
        ForeignKey(
            entity = TankEntity::class,
            parentColumns = ["id"],
            childColumns = ["tankId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["tankId"]), Index(value = ["operationalDayId"])]
)
data class InventoryMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val tankId: Long,
    val fuelTypeId: Long,
    val movementType: String, // "PURCHASE", "SALE", "ADJUSTMENT"
    val quantityLiters: Double, // positive for in, negative for out
    val balanceBeforeLiters: Double,
    val balanceAfterLiters: Double,
    val referenceType: String, // "SALE", "PURCHASE", "AUDIT"
    val referenceId: Long,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchases",
    foreignKeys = [
        ForeignKey(
            entity = OperationalDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["operationalDayId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["operationalDayId"]), Index(value = ["supplierId"])]
)
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val supplierId: Long,
    val fuelTypeId: Long,
    val tankId: Long,
    val quantityLiters: Double,
    val pricePerLiter: Double,
    val totalAmount: Double,
    val invoiceNumber: String,
    val purchaseDate: String,
    val paymentMethod: String = "CREDIT", // CASH, ACCOUNT, CREDIT
    val financialAccountId: Long? = null,
    val isPaid: Boolean = false,
    val notes: String = "",
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0, // Liability: positive means we owe supplier
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "supplier_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SupplierEntity::class,
            parentColumns = ["id"],
            childColumns = ["supplierId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["supplierId"]), Index(value = ["operationalDayId"])]
)
data class SupplierTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val supplierId: Long,
    val operationalDayId: Long = 0,
    val transactionType: String, // "supplier_due" (عليه للمورد), "station_due" (له للمحطة)
    val amount: Double,
    val balanceBefore: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val currency: String = "YER",
    val transactionDate: String = "",
    val details: String = "",
    val attachments: String = "",
    val status: String = "active", // "active", "cancelled", "reversed"
    val cancelReason: String? = null,
    val referenceInvoice: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val creditLimit: Double = 1000000.0,
    val initialBalance: Double = 0.0,
    val currentBalance: Double = 0.0, // Receivable: positive means customer owes station
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "customer_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"]), Index(value = ["operationalDayId"])]
)
data class CustomerTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val operationalDayId: Long,
    val transactionType: String, // "CREDIT_SALE", "PAYMENT"
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val referenceSaleId: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashbox")
data class CashboxEntity(
    @PrimaryKey val id: Long = 1,
    val name: String = "خزينة المحطة الرئيسية",
    val currentBalance: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cash_transactions",
    indices = [Index(value = ["operationalDayId"])]
)
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val voucherNumber: String,
    val transactionType: String, // "CASH_IN", "CASH_OUT", "SALE_REVENUE", "EXPENSE_PAYMENT", "CUSTOMER_PAYMENT", "SUPPLIER_PAYMENT"
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val reason: String,
    val beneficiary: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "financial_accounts")
data class FinancialAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // e.g. بنك الكريمي, محفظة جوالي
    val type: String, // "BANK", "WALLET", "OTHER"
    val accountNumber: String = "",
    val currentBalance: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "financial_transactions",
    indices = [Index(value = ["operationalDayId"]), Index(value = ["accountId"])]
)
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val accountId: Long,
    val relatedAccountId: Long? = null, // for transfers
    val transactionType: String, // "DEPOSIT", "WITHDRAWAL", "TRANSFER_IN", "TRANSFER_OUT", "SALE_REVENUE", "EXPENSE_PAYMENT"
    val amount: Double,
    val balanceBefore: Double,
    val balanceAfter: Double,
    val referenceNumber: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nameArabic: String,
    val isDefault: Boolean = true
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index(value = ["categoryId"]), Index(value = ["operationalDayId"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val categoryId: Long,
    val amount: Double,
    val expenseDate: String,
    val beneficiary: String,
    val paymentMethod: String = "CASHBOX", // CASHBOX, ACCOUNT
    val financialAccountId: Long? = null,
    val voucherNumber: String = "",
    val description: String = "",
    val notes: String = "",
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val jobTitle: String, // "عامل طرمبة", "محاسب", "فني صيانة", "حارس"
    val phone: String = "",
    val nationalId: String = "",
    val status: String = "ACTIVE", // ACTIVE, INACTIVE
    val salary: Double = 0.0,
    val notes: String = "",
    val hireDate: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "variances",
    indices = [Index(value = ["operationalDayId"])]
)
data class VarianceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationalDayId: Long,
    val varianceType: String, // "READING", "STOCK", "CASH", "SALES"
    val title: String,
    val expectedValue: Double,
    val actualValue: Double,
    val differenceValue: Double,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long = 1,
    val username: String = "مدير المحطة",
    val action: String, // "LOGIN", "CREATE", "UPDATE", "DELETE", "OPEN_DAY", "CLOSE_DAY", "INVENTORY_ADJUST", "FINANCIAL_TX"
    val entityName: String,
    val entityId: Long = 0,
    val oldValue: String = "",
    val newValue: String = "",
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
