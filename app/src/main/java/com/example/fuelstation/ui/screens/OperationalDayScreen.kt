package com.example.fuelstation.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OperationalDayScreen(
    activeDay: OperationalDayEntity?,
    allDays: List<OperationalDayEntity>,
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    cashTx: List<CashTransactionEntity>,
    variances: List<VarianceEntity>,
    cashbox: CashboxEntity?,
    onOpenDayClick: () -> Unit,
    onCloseDayClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: اليوم الحالي, 1: الأيام السابقة
    val currentCash = cashbox?.currentBalance ?: 0.0

    val cashSales = sales.filter { it.paymentMethod == "CASH" }.sumOf { it.totalAmount }
    val creditSales = sales.filter { it.paymentMethod == "CREDIT" }.sumOf { it.totalAmount }
    val accountSales = sales.filter { it.paymentMethod == "ACCOUNT" }.sumOf { it.totalAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceWhite,
            contentColor = Primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("اليوم التشغيلي الحالي", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل الأيام السابقة (${allDays.size})", fontWeight = FontWeight.Bold) }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Day Status Hero Card
                item {
                    val isOpen = activeDay?.status == "OPEN"
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOpen) Primary else DarkSlate
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "حالة اليوم التشغيلي",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SurfaceWhite
                                )
                                StatusBadge(
                                    text = if (isOpen) "مفتوح - جاري العمل" else "مغلق",
                                    backgroundColor = if (isOpen) SuccessGreen else WarningAmber,
                                    contentColor = if (isOpen) SurfaceWhite else DarkSlate
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "التاريخ: ${activeDay?.dayDate ?: "لا يوجد يوم نشط"}",
                                style = MaterialTheme.typography.headlineSmall.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold)
                            )
                            if (activeDay != null) {
                                val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                                Text(
                                    text = "تم الافتتاح في: ${sdf.format(Date(activeDay.openedAt))}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA7F3D0))
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isOpen) {
                                Button(
                                    onClick = onCloseDayClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.LockClock, contentDescription = null, tint = DarkSlate)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("إغلاق اليوم ومطابقة الجرد", color = DarkSlate, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = onOpenDayClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("افتتاح يوم تشغيلي جديد", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Financial & Sales Reconciliation Matrix
                item {
                    Text(
                        text = "ملخص مطابقة ومبيعات اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ReconciliationRow(label = "النقد الافتتاحي في الخزينة:", value = formatCurrency(activeDay?.openingCash ?: 0.0))
                            ReconciliationRow(label = "إجمالي كمية الوقود المباعة:", value = "${formatNumber(activeDay?.totalSalesLiters ?: 0.0)} لتر", isBold = true)
                            Divider(color = BorderColor)
                            ReconciliationRow(label = "إجمالي المبيعات الكلية:", value = formatCurrency(activeDay?.totalSalesAmount ?: 0.0), valueColor = Primary, isBold = true)
                            ReconciliationRow(label = "• مبيعات نقدية (كاش):", value = formatCurrency(cashSales))
                            ReconciliationRow(label = "• مبيعات حسابات بنكية / محافظ:", value = formatCurrency(accountSales))
                            ReconciliationRow(label = "• مبيعات آجلة (ذمم عملاء):", value = formatCurrency(creditSales))
                            Divider(color = BorderColor)
                            ReconciliationRow(label = "إجمالي المصروفات المسجلة:", value = "-${formatCurrency(activeDay?.totalExpenses ?: 0.0)}", valueColor = ErrorRed)
                            Divider(color = BorderColor)
                            ReconciliationRow(
                                label = "الرصيد الدفتري المتوقع للخزينة:",
                                value = formatCurrency(currentCash),
                                valueColor = BlueAccent,
                                isBold = true
                            )
                            if (activeDay?.closingActualCash != null) {
                                ReconciliationRow(
                                    label = "النقد الفعلي عند الإغلاق (الجرد):",
                                    value = formatCurrency(activeDay.closingActualCash),
                                    isBold = true
                                )
                                val diff = activeDay.cashDifference
                                val diffColor = when {
                                    diff > 0 -> SuccessGreen
                                    diff < 0 -> ErrorRed
                                    else -> Primary
                                }
                                ReconciliationRow(
                                    label = "فارق النقدية:",
                                    value = when {
                                        diff > 0 -> "+${formatCurrency(diff)} (فائض)"
                                        diff < 0 -> "${formatCurrency(diff)} (عجز)"
                                        else -> "مطابقة تامة (0)"
                                    },
                                    valueColor = diffColor,
                                    isBold = true
                                )
                            }
                        }
                    }
                }

                // Variances Section
                item {
                    Text(
                        text = "الفروقات المسجلة (الجرد والتسويات)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                if (variances.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("لا توجد فروقات أو عجز مسجل حتى الآن في هذا اليوم.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
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
                                    Text(v.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${formatNumber(v.differenceValue)} ر.ي",
                                        fontWeight = FontWeight.Bold,
                                        color = if (v.differenceValue < 0) ErrorRed else SuccessGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "المتوقع: ${formatNumber(v.expectedValue)} • الفعلي: ${formatNumber(v.actualValue)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                if (v.notes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(v.notes, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }

                // Cashbox Transactions of the Day
                item {
                    Text(
                        text = "حركات الخزينة في هذا اليوم (${cashTx.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                items(cashTx) { tx ->
                    val isIncome = tx.transactionType in listOf("CASH_IN", "SALE_REVENUE", "CUSTOMER_PAYMENT")
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
                                Text(tx.reason, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "سند: ${tx.voucherNumber} • الرصيد بعد: ${formatCurrency(tx.balanceAfter)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${if (isIncome) "+" else "-"}${formatCurrency(tx.amount)}",
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) SuccessGreen else ErrorRed
                            )
                        }
                    }
                }
            }
        } else {
            // Historical Days Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (allDays.isEmpty()) {
                    item {
                        EmptyState(title = "لا توجد أيام مسجلة", description = "سيظهر هنا سجل الأيام التشغيلية")
                    }
                } else {
                    items(allDays) { day ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "يوم: ${day.dayDate}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    StatusBadge(
                                        text = if (day.status == "OPEN") "مفتوح" else "مغلق",
                                        backgroundColor = if (day.status == "OPEN") SuccessGreen else BorderColor,
                                        contentColor = if (day.status == "OPEN") SurfaceWhite else DarkSlate
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المبيعات: ${formatCurrency(day.totalSalesAmount)}", style = MaterialTheme.typography.bodySmall, color = Primary)
                                    Text("الكمية: ${formatNumber(day.totalSalesLiters)} لتر", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المصروفات: ${formatCurrency(day.totalExpenses)}", style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                                    if (day.status == "CLOSED") {
                                        Text(
                                            "الفارق: ${formatNumber(day.cashDifference)} ر.ي",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (day.cashDifference < 0) ErrorRed else SuccessGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReconciliationRow(
    label: String,
    value: String,
    valueColor: Color = DarkSlate,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            color = DarkSlate
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
