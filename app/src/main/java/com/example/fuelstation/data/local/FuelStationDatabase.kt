package com.example.fuelstation.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.fuelstation.data.local.dao.FuelStationDao
import com.example.fuelstation.data.local.entities.*

@Database(
    entities = [
        UserEntity::class,
        StationEntity::class,
        OperationalDayEntity::class,
        FuelTypeEntity::class,
        FuelPriceEntity::class,
        PumpEntity::class,
        NozzleEntity::class,
        TankEntity::class,
        ReadingEntity::class,
        SaleEntity::class,
        InventoryMovementEntity::class,
        PurchaseEntity::class,
        SupplierEntity::class,
        SupplierTransactionEntity::class,
        CustomerEntity::class,
        CustomerTransactionEntity::class,
        CashboxEntity::class,
        CashTransactionEntity::class,
        FinancialAccountEntity::class,
        FinancialTransactionEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        EmployeeEntity::class,
        VarianceEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FuelStationDatabase : RoomDatabase() {
    abstract fun dao(): FuelStationDao

    companion object {
        @Volatile
        private var INSTANCE: FuelStationDatabase? = null

        fun getDatabase(context: Context): FuelStationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FuelStationDatabase::class.java,
                    "fuel_station_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
