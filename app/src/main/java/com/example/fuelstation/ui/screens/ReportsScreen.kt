package com.example.fuelstation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class ReportCategory(val title: String) {
    DAILY("التقرير اليومي الشامل"),
    SALES("تقرير المبيعات والوقود"),
    EXPENSES("تقرير المصروفات"),
    INVENTORY("تقرير الخزانات والمخزون"),
    CASHBOX("تقرير الخزينة والسيولة"),
    CUSTOMERS("تقرير ذمم العملاء"),
    SUPPLIERS("تقرير التزامات الموردين"),
    VARIANCES("تقرير الفروقات والتسويات")
}

@Composable
fun ReportsScreen(
    station: StationEntity?,
    activeDay: OperationalDayEntity?,
    allDays: List<OperationalDayEntity>,
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    fuelTypes: List<FuelTypeEntity>,
    tanks: List<TankEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    cashbox: CashboxEntity?,
    variances: List<VarianceEntity>
) {
    var selectedCategory by remember { mutableStateOf(ReportCategory.DAILY) }
    var showPrintDialog by remember { mutableStateOf(false) }

    val totalSalesAmount = sales.sumOf { it.totalAmount }
    val totalSalesLiters = sales.sumOf { it.quantityLiters }
    val totalExpensesAmount = expenses.sumOf { it.amount }
    val netCashFlow = totalSalesAmount - totalExpensesAmount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Header with Print/Export action
        Surface(
            color = SurfaceWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("التقارير والمطابقات المالية", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("تقارير دقيقة ومحدثة آنياً من قاعدة البيانات", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }

                Button(
                    onClick = { showPrintDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("طباعة / تصدير")
                }
            }
        }

        // Horizontal Category Selector Chips
        ScrollableTabRow(
            selectedTabIndex = selectedCategory.ordinal,
            containerColor = SurfaceWhite,
            contentColor = Primary,
            edgePadding = 16.dp
        ) {
            ReportCategory.values().forEach { cat ->
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(cat.title, fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        // Content Area based on Category
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedCategory) {
                ReportCategory.DAILY -> {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("ملخص التشغيل لليوم: ${activeDay?.dayDate ?: "اليوم الحالي"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkSlate)
                                Divider(color = BorderColor)
                                ReconciliationRow(label = "إجمالي كمية الوقود المباعة:", value = "${formatNumber(totalSalesLiters)} لتر", isBold = true)
                                ReconciliationRow(label = "إجمالي إيراد المبيعات:", value = formatCurrency(totalSalesAmount), valueColor = Primary, isBold = true)
                                ReconciliationRow(label = "إجمالي المصروفات التشغيلية:", value = "-${formatCurrency(totalExpensesAmount)}", valueColor = ErrorRed, isBold = true)
                                Divider(color = BorderColor)
                                ReconciliationRow(
                                    label = "صافي التدفق المالي التقديري:",
                                    value = formatCurrency(netCashFlow),
                                    valueColor = if (netCashFlow >= 0) SuccessGreen else ErrorRed,
                                    isBold = true
                                )
                                ReconciliationRow(label = "رصيد الخزينة الحالي:", value = formatCurrency(cashbox?.currentBalance ?: 0.0), isBold = true)
                            }
                        }
                    }

                    item {
                        Text("تفصيل المبيعات حسب نوع الوقود", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(fuelTypes) { fuel ->
                        val fuelSales = sales.filter { it.fuelTypeId == fuel.id }
                        val liters = fuelSales.sumOf { it.quantityLiters }
                        val amount = fuelSales.sumOf { it.totalAmount }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(fuel.nameArabic, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text("الكمية: ${formatNumber(liters)} لتر (${fuelSales.size} عمليات)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(formatCurrency(amount), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Primary)
                            }
                        }
                    }
                }

                ReportCategory.SALES -> {
                    item {
                        StatCard(
                            title = "إجمالي مبيعات اليوم",
                            value = formatCurrency(totalSalesAmount),
                            subtitle = "${formatNumber(totalSalesLiters)} لتر عبر ${sales.size} عمليات",
                            icon = Icons.Default.TrendingUp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(sales) { sale ->
                        val fuel = fuelTypes.firstOrNull { it.id == sale.fuelTypeId }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("${fuel?.nameArabic ?: "وقود"} • ${formatNumber(sale.quantityLiters)} لتر", fontWeight = FontWeight.SemiBold)
                                    Text("سعر اللتر: ${formatCurrency(sale.unitPrice)} • طريقة الدفع: ${sale.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                                Text(formatCurrency(sale.totalAmount), fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                    }
                }

                ReportCategory.EXPENSES -> {
                    item {
                        StatCard(
                            title = "إجمالي المصروفات التشغيلية",
                            value = formatCurrency(totalExpensesAmount),
                            subtitle = "${expenses.size} سندات مصروفات",
                            icon = Icons.Default.ReceiptLong,
                            iconTint = ErrorRed,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(expenses) { exp ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(exp.beneficiary, fontWeight = FontWeight.Bold)
                                    Text(formatCurrency(exp.amount), fontWeight = FontWeight.Bold, color = ErrorRed)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(exp.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text("طريقة الصرف: ${if (exp.paymentMethod == "CASHBOX") "الخزينة" else "حساب بنكي"} • سند: ${exp.voucherNumber}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                        }
                    }
                }

                ReportCategory.CUSTOMERS -> {
                    val totalDebt = customers.sumOf { it.currentBalance }
                    item {
                        StatCard(
                            title = "إجمالي الذمم والديون على العملاء",
                            value = formatCurrency(totalDebt),
                            subtitle = "${customers.size} عملاء مسجلين",
                            icon = Icons.Default.People,
                            iconTint = Color(0xFF8B5CF6),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(customers) { c ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.Bold)
                                    Text("هاتف: ${c.phone.ifBlank { "غير مسجل" }}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الرصيد المستحق:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(formatCurrency(c.currentBalance), fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                }
                            }
                        }
                    }
                }

                ReportCategory.SUPPLIERS -> {
                    val totalPayable = suppliers.sumOf { it.currentBalance }
                    item {
                        StatCard(
                            title = "إجمالي الالتزامات المستحقة للموردين",
                            value = formatCurrency(totalPayable),
                            subtitle = "${suppliers.size} موردين مسجلين",
                            icon = Icons.Default.Business,
                            iconTint = BlueAccent,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(suppliers) { sup ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(sup.name, fontWeight = FontWeight.Bold)
                                    Text("هاتف: ${sup.phone.ifBlank { "غير مسجل" }}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("المبلغ المستحق للمورد:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(formatCurrency(sup.currentBalance), fontWeight = FontWeight.Bold, color = BlueAccent)
                                }
                            }
                        }
                    }
                }

                ReportCategory.INVENTORY -> {
                    items(tanks) { t ->
                        val fuel = fuelTypes.firstOrNull { it.id == t.fuelTypeId }
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(t.name, fontWeight = FontWeight.Bold)
                                Text("الوقود: ${fuel?.nameArabic ?: ""} • الرصيد: ${formatNumber(t.currentStockLiters)} / ${formatNumber(t.capacityLiters)} لتر", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                ReportCategory.CASHBOX -> {
                    item {
                        StatCard(
                            title = "رصيد الخزينة الحالي",
                            value = formatCurrency(cashbox?.currentBalance ?: 0.0),
                            subtitle = "النقدية الفعلية المتاحة في الصندوق",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                ReportCategory.VARIANCES -> {
                    if (variances.isEmpty()) {
                        item {
                            EmptyState(title = "لا توجد فروقات مسجلة", description = "كافة العمليات متطابقة دفترياً وفعلياً.")
                        }
                    } else {
                        items(variances) { v ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(v.title, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = formatCurrency(v.differenceValue),
                                            fontWeight = FontWeight.Bold,
                                            color = if (v.differenceValue < 0) ErrorRed else SuccessGreen
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("المتوقع: ${formatNumber(v.expectedValue)} • الفعلي: ${formatNumber(v.actualValue)}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    if (v.notes.isNotBlank()) Text(v.notes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Printable Summary Dialog
    if (showPrintDialog) {
        AlertDialog(
            onDismissRequest = { showPrintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تقرير المحطة المالي والتشغيلي", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("اسم المنشأة: ${station?.name ?: "محطة الوقود"}", fontWeight = FontWeight.Bold)
                    Text("التاريخ: ${activeDay?.dayDate ?: "2026-10-02"}")
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = BorderColor)
                    Text("• إجمالي مبيعات الوقود: ${formatCurrency(totalSalesAmount)} (${formatNumber(totalSalesLiters)} لتر)")
                    Text("• إجمالي المصروفات: ${formatCurrency(totalExpensesAmount)}")
                    Text("• رصيد الخزينة الفعلي: ${formatCurrency(cashbox?.currentBalance ?: 0.0)}")
                    Text("• رصيد ذمم العملاء (آجل): ${formatCurrency(customers.sumOf { it.currentBalance })}")
                    Text("• التزامات الموردين: ${formatCurrency(suppliers.sumOf { it.currentBalance })}")
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = BorderColor)
                    Text("تم إنشاء هذا التقرير آلياً من سجلات النظام وقاعدة البيانات.", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showPrintDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("إغلاق / تم التصدير") }
            }
        )
    }
}
