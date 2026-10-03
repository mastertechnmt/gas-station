package com.example.fuelstation.data.seed

import com.example.fuelstation.data.local.dao.FuelStationDao
import com.example.fuelstation.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatabaseSeeder {

    suspend fun seedIfNeeded(dao: FuelStationDao) = withContext(Dispatchers.IO) {
        try {
            val existingStation = dao.getStationSync()
            if (existingStation != null) return@withContext

        // 1. Station info
        dao.insertOrUpdateStation(
            StationEntity(
                id = 1,
                name = "محطة الوقود الرئيسية",
                address = "صنعاء - الجمهورية اليمنية",
                phone = "+967 770 123 456",
                currency = "ريال يمني",
                currencySymbol = "ر.ي",
                timezone = "Asia/Aden",
                openingHour = "06:00",
                closingHour = "23:00"
            )
        )

        // 2. Manager User
        dao.insertUser(
            UserEntity(
                id = 1,
                username = "admin",
                passwordHash = "123456", // Simple hash/pin for demo manager
                fullName = "مدير المحطة",
                role = "MANAGER",
                isActive = true
            )
        )

        // 3. Fuel Types
        val fuel91Id = dao.insertFuelType(
            FuelTypeEntity(
                code = "G91",
                nameArabic = "بنزين 91",
                nameEnglish = "Gasoline 91",
                colorHex = "#10B981", // Green
                currentPrice = 400.0,
                isAvailable = true
            )
        )
        dao.insertFuelPrice(
            FuelPriceEntity(
                fuelTypeId = fuel91Id,
                pricePerLiter = 400.0,
                effectiveFrom = System.currentTimeMillis() - 86400000L * 7,
                notes = "السعر الرسمي المعتمد"
            )
        )

        val fuel95Id = dao.insertFuelType(
            FuelTypeEntity(
                code = "G95",
                nameArabic = "بنزين 95 ممتاز",
                nameEnglish = "Gasoline 95 Super",
                colorHex = "#0284C7", // Sky Blue
                currentPrice = 450.0,
                isAvailable = true
            )
        )
        dao.insertFuelPrice(
            FuelPriceEntity(
                fuelTypeId = fuel95Id,
                pricePerLiter = 450.0,
                effectiveFrom = System.currentTimeMillis() - 86400000L * 7,
                notes = "السعر الرسمي المعتمد"
            )
        )

        val dieselId = dao.insertFuelType(
            FuelTypeEntity(
                code = "DSL",
                nameArabic = "ديزل",
                nameEnglish = "Diesel",
                colorHex = "#F59E0B", // Amber
                currentPrice = 380.0,
                isAvailable = true
            )
        )
        dao.insertFuelPrice(
            FuelPriceEntity(
                fuelTypeId = dieselId,
                pricePerLiter = 380.0,
                effectiveFrom = System.currentTimeMillis() - 86400000L * 7,
                notes = "السعر الرسمي المعتمد"
            )
        )

        // 4. Tanks
        val tank91Id = dao.insertTank(
            TankEntity(
                tankNumber = 1,
                name = "خزان بنزين 91 الرئيسي",
                fuelTypeId = fuel91Id,
                capacityLiters = 50000.0,
                currentStockLiters = 22400.0,
                minAlertLiters = 5000.0,
                criticalAlertLiters = 2000.0
            )
        )

        val tank95Id = dao.insertTank(
            TankEntity(
                tankNumber = 2,
                name = "خزان بنزين 95 ممتاز",
                fuelTypeId = fuel95Id,
                capacityLiters = 35000.0,
                currentStockLiters = 14800.0,
                minAlertLiters = 4000.0,
                criticalAlertLiters = 1500.0
            )
        )

        val tankDieselId = dao.insertTank(
            TankEntity(
                tankNumber = 3,
                name = "خزان ديزل رئيسي",
                fuelTypeId = dieselId,
                capacityLiters = 60000.0,
                currentStockLiters = 31200.0,
                minAlertLiters = 6000.0,
                criticalAlertLiters = 2500.0
            )
        )

        // 5. Pumps & Nozzles
        val pump1Id = dao.insertPump(PumpEntity(pumpNumber = 1, name = "طرمبة 1 (شمالية)", status = "ACTIVE"))
        val pump2Id = dao.insertPump(PumpEntity(pumpNumber = 2, name = "طرمبة 2 (جنوبية)", status = "ACTIVE"))

        dao.insertNozzle(
            NozzleEntity(
                pumpId = pump1Id,
                nozzleNumber = 1,
                fuelTypeId = fuel91Id,
                tankId = tank91Id,
                lastReading = 125450.0,
                status = "ACTIVE"
            )
        )
        dao.insertNozzle(
            NozzleEntity(
                pumpId = pump1Id,
                nozzleNumber = 2,
                fuelTypeId = fuel95Id,
                tankId = tank95Id,
                lastReading = 84200.0,
                status = "ACTIVE"
            )
        )

        dao.insertNozzle(
            NozzleEntity(
                pumpId = pump2Id,
                nozzleNumber = 1,
                fuelTypeId = dieselId,
                tankId = tankDieselId,
                lastReading = 310100.0,
                status = "ACTIVE"
            )
        )
        dao.insertNozzle(
            NozzleEntity(
                pumpId = pump2Id,
                nozzleNumber = 2,
                fuelTypeId = fuel91Id,
                tankId = tank91Id,
                lastReading = 95800.0,
                status = "ACTIVE"
            )
        )

        // 6. Cashbox & Financial Accounts
        dao.insertOrUpdateCashbox(
            CashboxEntity(
                id = 1,
                name = "خزينة المحطة الرئيسية",
                currentBalance = 350000.0
            )
        )

        dao.insertFinancialAccount(
            FinancialAccountEntity(
                name = "بنك الكريمي للتمويل الأصغر",
                type = "BANK",
                accountNumber = "120345678",
                currentBalance = 1500000.0
            )
        )
        dao.insertFinancialAccount(
            FinancialAccountEntity(
                name = "محفظة جوالي - كاك بنك",
                type = "WALLET",
                accountNumber = "770112233",
                currentBalance = 450000.0
            )
        )

        // 7. Expense Categories
        val defaultCategories = listOf(
            "صيانة وتشغيل",
            "رواتب وحوافز",
            "كهرباء وطاقة",
            "مياه وخدمات",
            "نقل ومواصلات",
            "مشتريات مستهلكة",
            "مصروفات نثرية وإدارية"
        )
        for (cat in defaultCategories) {
            dao.insertExpenseCategory(ExpenseCategoryEntity(nameArabic = cat, isDefault = true))
        }

        // 8. Demo Customers & Suppliers
        val customerId = dao.insertCustomer(
            CustomerEntity(
                name = "شركة النقل البري السريع",
                phone = "777123456",
                address = "حي المطار، صنعاء",
                creditLimit = 1500000.0,
                initialBalance = 300000.0,
                currentBalance = 300000.0, // Owed to station
                notes = "عميل آجل معتمد"
            )
        )
        dao.insertCustomer(
            CustomerEntity(
                name = "مؤسسة الوفاء للتجارة",
                phone = "771998877",
                address = "شارع الستين",
                creditLimit = 800000.0,
                initialBalance = 120000.0,
                currentBalance = 120000.0,
                notes = "سداد دوري نهاية كل أسبوع"
            )
        )

        val supplierId = dao.insertSupplier(
            SupplierEntity(
                name = "شركة النفط الوطنية",
                phone = "770987654",
                address = "الإدارة العامة، صنعاء",
                initialBalance = 1200000.0,
                currentBalance = 1200000.0, // Station owes supplier
                notes = "المورد الرئيسي للمشتقات النفطية"
            )
        )

        // 9. Demo Employees
        dao.insertEmployee(
            EmployeeEntity(
                name = "أحمد ناصر القدسي",
                jobTitle = "عامل طرمبة (نوبة صباحية)",
                phone = "773112244",
                nationalId = "0102030405",
                salary = 120000.0,
                hireDate = "2024-01-15"
            )
        )
        dao.insertEmployee(
            EmployeeEntity(
                name = "محمد علي الحيمي",
                jobTitle = "عامل طرمبة (نوبة مسائية)",
                phone = "775223355",
                nationalId = "0203040506",
                salary = 120000.0,
                hireDate = "2024-03-01"
            )
        )
        dao.insertEmployee(
            EmployeeEntity(
                name = "صالح العمري",
                jobTitle = "فني صيانة ومضخات",
                phone = "771334466",
                nationalId = "0304050607",
                salary = 150000.0,
                hireDate = "2023-11-10"
            )
        )

        // 10. Open Operational Day for today
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        val dayId = dao.insertDay(
            OperationalDayEntity(
                dayDate = todayStr,
                openedAt = System.currentTimeMillis() - 14400000L, // opened 4 hours ago
                status = "OPEN",
                openingCash = 350000.0,
                expectedCash = 350000.0,
                notes = "اليوم التشغيلي الافتراضي"
            )
        )

        // Initial inventory movement records for current balances
        dao.insertInventoryMovement(
            InventoryMovementEntity(
                operationalDayId = dayId,
                tankId = tank91Id,
                fuelTypeId = fuel91Id,
                movementType = "ADJUSTMENT",
                quantityLiters = 22400.0,
                balanceBeforeLiters = 0.0,
                balanceAfterLiters = 22400.0,
                referenceType = "INITIAL_BALANCE",
                referenceId = 0,
                notes = "الرصيد الافتتاحي للخزان"
            )
        )
        dao.insertInventoryMovement(
            InventoryMovementEntity(
                operationalDayId = dayId,
                tankId = tank95Id,
                fuelTypeId = fuel95Id,
                movementType = "ADJUSTMENT",
                quantityLiters = 14800.0,
                balanceBeforeLiters = 0.0,
                balanceAfterLiters = 14800.0,
                referenceType = "INITIAL_BALANCE",
                referenceId = 0,
                notes = "الرصيد الافتتاحي للخزان"
            )
        )
        dao.insertInventoryMovement(
            InventoryMovementEntity(
                operationalDayId = dayId,
                tankId = tankDieselId,
                fuelTypeId = dieselId,
                movementType = "ADJUSTMENT",
                quantityLiters = 31200.0,
                balanceBeforeLiters = 0.0,
                balanceAfterLiters = 31200.0,
                referenceType = "INITIAL_BALANCE",
                referenceId = 0,
                notes = "الرصيد الافتتاحي للخزان"
            )
        )

        // Initial audit log
        dao.insertAuditLog(
            AuditLogEntity(
                userId = 1,
                username = "مدير المحطة",
                action = "OPEN_DAY",
                entityName = "operational_days",
                entityId = dayId,
                details = "افتتاح النظام وتهيئة اليوم التشغيلي"
            )
        )
        } catch (e: Exception) {
            android.util.Log.e("DatabaseSeeder", "Error during seeding", e)
        }
    }
}
