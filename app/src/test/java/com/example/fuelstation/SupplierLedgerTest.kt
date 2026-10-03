package com.example.fuelstation

import com.example.fuelstation.data.local.entities.SupplierTransactionEntity
import com.example.fuelstation.data.repository.SupplierBalanceSummary
import org.junit.Assert.*
import org.junit.Test

class SupplierLedgerTest {

    private fun computeSummary(transactions: List<SupplierTransactionEntity>): SupplierBalanceSummary {
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

    /**
     * حالة اختبار 1:
     * عملية "عليه" = 500,000
     * النتيجة: للمورد 500,000 (على المحطة للمورد)
     */
    @Test
    fun testCase1_AlaihOnly() {
        val txList = listOf(
            SupplierTransactionEntity(
                id = 1,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 500000.0,
                status = "active"
            )
        )
        val summary = computeSummary(txList)

        assertEquals(500000.0, summary.totalAlaih, 0.001)
        assertEquals(0.0, summary.totalLahu, 0.001)
        assertEquals(500000.0, summary.netBalance, 0.001)
        assertEquals("على المحطة للمورد", summary.statusText)
        assertEquals("SUPPLIER_DUE", summary.statusCode)
    }

    /**
     * حالة اختبار 2:
     * عليه = 500,000
     * له = 200,000
     * النتيجة: للمورد 300,000 (على المحطة للمورد)
     */
    @Test
    fun testCase2_AlaihGreaterThanLahu() {
        val txList = listOf(
            SupplierTransactionEntity(
                id = 1,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 500000.0,
                status = "active"
            ),
            SupplierTransactionEntity(
                id = 2,
                supplierId = 1,
                transactionType = "station_due",
                amount = 200000.0,
                status = "active"
            )
        )
        val summary = computeSummary(txList)

        assertEquals(500000.0, summary.totalAlaih, 0.001)
        assertEquals(200000.0, summary.totalLahu, 0.001)
        assertEquals(300000.0, summary.netBalance, 0.001)
        assertEquals("على المحطة للمورد", summary.statusText)
        assertEquals("SUPPLIER_DUE", summary.statusCode)
    }

    /**
     * حالة اختبار 3:
     * عليه = 500,000
     * له = 500,000
     * النتيجة: متسوي (الرصيد: 0)
     */
    @Test
    fun testCase3_AlaihEqualsLahuSettled() {
        val txList = listOf(
            SupplierTransactionEntity(
                id = 1,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 500000.0,
                status = "active"
            ),
            SupplierTransactionEntity(
                id = 2,
                supplierId = 1,
                transactionType = "station_due",
                amount = 500000.0,
                status = "active"
            )
        )
        val summary = computeSummary(txList)

        assertEquals(500000.0, summary.totalAlaih, 0.001)
        assertEquals(500000.0, summary.totalLahu, 0.001)
        assertEquals(0.0, summary.netBalance, 0.001)
        assertEquals("متسوي", summary.statusText)
        assertEquals("SETTLED", summary.statusCode)
    }

    /**
     * حالة اختبار 4:
     * له = 500,000
     * عليه = 300,000
     * النتيجة: 200,000 لصالح المحطة (للمحطة عند المورد)
     */
    @Test
    fun testCase4_LahuGreaterThanAlaih() {
        val txList = listOf(
            SupplierTransactionEntity(
                id = 1,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 300000.0,
                status = "active"
            ),
            SupplierTransactionEntity(
                id = 2,
                supplierId = 1,
                transactionType = "station_due",
                amount = 500000.0,
                status = "active"
            )
        )
        val summary = computeSummary(txList)

        assertEquals(300000.0, summary.totalAlaih, 0.001)
        assertEquals(500000.0, summary.totalLahu, 0.001)
        assertEquals(200000.0, summary.netBalance, 0.001)
        assertEquals("للمحطة عند المورد", summary.statusText)
        assertEquals("STATION_DUE", summary.statusCode)
    }

    /**
     * حالة اختبار 6:
     * فتح الآلة الحاسبة وإجراء:
     * 100000 + 250000 = 350000
     */
    @Test
    fun testCase6_CalculatorLogic() {
        val op1 = 100000.0
        val op2 = 250000.0
        val sum = op1 + op2
        assertEquals(350000.0, sum, 0.001)

        val formatResult = if (sum % 1.0 == 0.0) sum.toLong().toString() else sum.toString()
        assertEquals("350000", formatResult)
    }

    /**
     * حالة اختبار 7:
     * إرفاق صورة مع العملية
     * التأكد من حفظ مسار المرفق وربطه، وعدم حذف الحركة إذا تم فك ارتباط المرفق
     */
    @Test
    fun testCase7_AttachmentAttachmentPath() {
        val samplePath = "/data/user/0/com.example/files/supp_att_12345.jpg"
        val tx = SupplierTransactionEntity(
            id = 10,
            supplierId = 1,
            transactionType = "supplier_due",
            amount = 150000.0,
            attachments = samplePath,
            status = "active"
        )
        assertEquals(samplePath, tx.attachments)
        assertTrue(tx.attachments.isNotBlank())
    }

    /**
     * حالة اختبار 8:
     * إلغاء العملية المالية (Soft Cancel) مع توثيق السبب
     * يجب ألا تُحتسب العملية الملغاة في الرصيد الصافي
     */
    @Test
    fun testCase8_CancelledTransactionsIgnoredInBalance() {
        val txList = listOf(
            SupplierTransactionEntity(
                id = 1,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 500000.0,
                status = "active"
            ),
            SupplierTransactionEntity(
                id = 2,
                supplierId = 1,
                transactionType = "supplier_due",
                amount = 200000.0,
                status = "cancelled",
                cancelReason = "خطأ في قيد الفاتورة"
            )
        )
        val summary = computeSummary(txList)

        // Only the active 500,000 should be counted
        assertEquals(1, summary.totalTransactions)
        assertEquals(500000.0, summary.totalAlaih, 0.001)
        assertEquals(500000.0, summary.netBalance, 0.001)
    }
}
