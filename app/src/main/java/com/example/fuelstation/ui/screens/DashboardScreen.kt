package com.example.fuelstation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    station: StationEntity?,
    activeDay: OperationalDayEntity?,
    tanks: List<TankEntity>,
    fuelTypes: List<FuelTypeEntity>,
    cashbox: CashboxEntity?,
    sales: List<SaleEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    pumps: List<PumpEntity>,
    nozzles: List<NozzleEntity>,
    onOpenDayClick: () -> Unit,
    onCloseDayClick: () -> Unit,
    onRecordReadingClick: () -> Unit,
    onAddPurchaseClick: () -> Unit,
    onRecordExpenseClick: () -> Unit,
    onCashMovementClick: () -> Unit
) {
    val totalTodaySalesAmount = sales.sumOf { it.totalAmount }
    val totalTodaySalesLiters = sales.sumOf { it.quantityLiters }
    val totalTodayExpenses = expenses.sumOf { it.amount }
    val totalReceivables = customers.sumOf { it.currentBalance }
    val currentCashBalance = cashbox?.currentBalance ?: 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Station Header & Operational Day Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = station?.name ?: "محطة الوقود الرئيسية",
                                style = MaterialTheme.typography.titleMedium.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = station?.address ?: "الجمهورية اليمنية",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA7F3D0))
                            )
                        }
                        if (activeDay != null && activeDay.status == "OPEN") {
                            StatusBadge(
                                text = "اليوم مفتوح",
                                backgroundColor = SuccessGreen,
                                contentColor = SurfaceWhite
                            )
                        } else {
                            StatusBadge(
                                text = "اليوم مغلق",
                                backgroundColor = WarningAmber,
                                contentColor = DarkSlate
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "تاريخ اليوم التشغيلي:",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFA7F3D0))
                            )
                            Text(
                                text = activeDay?.dayDate ?: "لا يوجد يوم نشط",
                                style = MaterialTheme.typography.titleSmall.copy(color = SurfaceWhite, fontWeight = FontWeight.Bold)
                            )
                        }

                        if (activeDay != null && activeDay.status == "OPEN") {
                            FilledTonalButton(
                                onClick = onCloseDayClick,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SurfaceWhite,
                                    contentColor = Primary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إغلاق اليوم", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onOpenDayClick,
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("افتتاح اليوم", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Primary KPI Cards Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "مبيعات اليوم",
                        value = formatCurrency(totalTodaySalesAmount),
                        subtitle = "${formatNumber(totalTodaySalesLiters)} لتر وقود مباع",
                        icon = Icons.Default.TrendingUp,
                        iconTint = Primary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "رصيد الخزينة",
                        value = formatCurrency(currentCashBalance),
                        subtitle = "النقد الدفتري المتاح",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = BlueAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "مصروفات اليوم",
                        value = formatCurrency(totalTodayExpenses),
                        subtitle = "${expenses.size} سندات مصروفات",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "ديون العملاء (آجل)",
                        value = formatCurrency(totalReceivables),
                        subtitle = "${customers.size} عملاء مسجلين",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Quick Action Buttons Bar
        item {
            Text(
                text = "إجراءات سريعة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Speed,
                    label = "تسجيل قراءة",
                    color = Primary,
                    onClick = onRecordReadingClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.LocalShipping,
                    label = "إضافة توريد",
                    color = BlueAccent,
                    onClick = onAddPurchaseClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Receipt,
                    label = "تسجيل مصروف",
                    color = WarningAmber,
                    onClick = onRecordExpenseClick,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Payments,
                    label = "حركة نقدية",
                    color = Color(0xFF0D9488),
                    onClick = onCashMovementClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Fuel Tanks Live Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مستوى مخزون الخزانات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                Text(
                    text = "${tanks.size} خزانات",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        items(tanks) { tank ->
            val fuel = fuelTypes.firstOrNull { it.id == tank.fuelTypeId }
            val percentage = if (tank.capacityLiters > 0) (tank.currentStockLiters / tank.capacityLiters).toFloat() else 0f
            val status = when {
                tank.currentStockLiters <= tank.criticalAlertLiters -> "حرج"
                tank.currentStockLiters <= tank.minAlertLiters -> "منخفض"
                else -> "مستقر"
            }
            val statusColor = when (status) {
                "حرج" -> ErrorRed
                "منخفض" -> WarningAmber
                else -> SuccessGreen
            }

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
                        Column {
                            Text(tank.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            if (fuel != null) {
                                FuelTypeBadge(name = fuel.nameArabic, code = fuel.code, colorHex = fuel.colorHex)
                            }
                        }
                        StatusBadge(
                            text = status,
                            backgroundColor = statusColor.copy(alpha = 0.15f),
                            contentColor = statusColor
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { percentage.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = statusColor,
                        trackColor = BorderColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "الرصيد: ${formatNumber(tank.currentStockLiters)} لتر",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkSlate
                        )
                        Text(
                            text = "السعة: ${formatNumber(tank.capacityLiters)} لتر (${(percentage * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 5. Recent Sales Operations
        item {
            Text(
                text = "آخر عمليات المبيعات المسجلة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = DarkSlate
            )
        }

        if (sales.isEmpty()) {
            item {
                EmptyState(
                    title = "لا توجد مبيعات مسجلة في هذا اليوم",
                    description = "استخدم زر 'تسجيل قراءة' لإدخال قراءات العدادات واحتساب المبيعات تلقائياً."
                )
            }
        } else {
            items(sales.take(5)) { sale ->
                val fuel = fuelTypes.firstOrNull { it.id == sale.fuelTypeId }
                val pump = pumps.firstOrNull { it.id == sale.pumpId }
                val nozzle = nozzles.firstOrNull { it.id == sale.nozzleId }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalGasStation, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${pump?.name ?: "طرمبة"} - مسدس #${nozzle?.nozzleNumber ?: 1}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${fuel?.nameArabic ?: ""} • ${formatNumber(sale.quantityLiters)} لتر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatCurrency(sale.totalAmount),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Primary
                            )
                            val paymentLabel = when (sale.paymentMethod) {
                                "CASH" -> "نقداً"
                                "ACCOUNT" -> "حساب بنكي"
                                "CREDIT" -> "آجل"
                                else -> sale.paymentMethod
                            }
                            Text(
                                text = paymentLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = SurfaceWhite,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = DarkSlate,
                maxLines = 1
            )
        }
    }
}
