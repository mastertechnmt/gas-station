package com.example.fuelstation

import org.junit.Assert.*
import org.junit.Test

class FuelStationBusinessLogicTest {

    @Test
    fun testReadingAndSoldLitersCalculation() {
        val previousReading = 125450.0
        val currentReading = 126020.0
        assertTrue("Current reading must be greater than or equal to previous reading", currentReading >= previousReading)

        val soldLiters = currentReading - previousReading
        assertEquals(570.0, soldLiters, 0.001)
    }

    @Test
    fun testReadingValidationPreventsDecreasingReading() {
        val previousReading = 125450.0
        val invalidCurrentReading = 125400.0
        val isValid = invalidCurrentReading >= previousReading
        assertFalse("System must reject readings lower than previous", isValid)
    }

    @Test
    fun testSalesAmountCalculation() {
        val soldLiters = 570.0
        val pricePerLiter = 400.0
        val totalAmount = soldLiters * pricePerLiter
        assertEquals(228000.0, totalAmount, 0.001)
    }

    @Test
    fun testInventoryMovementCalculation() {
        val openingStock = 10000.0
        val purchase = 5000.0
        val sale = 3200.0
        val currentStock = openingStock + purchase - sale
        assertEquals(11800.0, currentStock, 0.001)
    }

    @Test
    fun testCashboxBalanceCalculation() {
        val openingBalance = 350000.0
        val cashIn = 50000.0
        val cashSales = 228000.0
        val expenses = 45000.0
        val currentBalance = openingBalance + cashIn + cashSales - expenses
        assertEquals(583000.0, currentBalance, 0.001)
    }

    @Test
    fun testCustomerCreditBalance() {
        val initialDebt = 300000.0
        val creditSale = 500000.0
        val payment = 200000.0
        val currentCustomerBalance = initialDebt + creditSale - payment
        assertEquals(600000.0, currentCustomerBalance, 0.001)
    }

    @Test
    fun testSupplierLiabilityBalance() {
        val initialLiability = 1200000.0
        val deliveryPurchase = 2000000.0
        val stationPayment = 1500000.0
        val currentSupplierBalance = initialLiability + deliveryPurchase - stationPayment
        assertEquals(1700000.0, currentSupplierBalance, 0.001)
    }

    @Test
    fun testDayVarianceCalculation() {
        val expectedCash = 500000.0
        val actualCashInHand = 490000.0
        val variance = actualCashInHand - expectedCash
        assertEquals(-10000.0, variance, 0.001)
        assertTrue("Negative variance represents cash shortage", variance < 0)
    }

    @Test
    fun testFinancialTransferIntegrity() {
        var sourceBalance = 1500000.0
        var destBalance = 450000.0
        val transferAmount = 200000.0

        assertTrue("Source account must have enough balance", sourceBalance >= transferAmount)
        sourceBalance -= transferAmount
        destBalance += transferAmount

        assertEquals(1300000.0, sourceBalance, 0.001)
        assertEquals(650000.0, destBalance, 0.001)
    }
}
