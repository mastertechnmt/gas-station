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

@Composable
fun InventoryScreen(
    tanks: List<TankEntity>,
    fuelTypes: List<FuelTypeEntity>,
    purchases: List<PurchaseEntity>,
    suppliers: List<SupplierEntity>,
    onAddPurchaseClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: الخزانات والمخزون, 1: التوريدات والشراء

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Tab Selector
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceWhite,
            contentColor = Primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("مخزون الخزانات (${tanks.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل التوريدات (${purchases.size})", fontWeight = FontWeight.Bold) }
            )
        }

        // Action Header
        Surface(
            color = SurfaceWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "إدارة المخزون والتوريدات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                Button(
                    onClick = onAddPurchaseClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة توريد وقود", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (selectedTab == 0) {
            // Tanks Inventory View
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(tank.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
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

                            Spacer(modifier = Modifier.height(14.dp))

                            // Gauge / Progress
                            LinearProgressIndicator(
                                progress = { percentage.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = statusColor,
                                trackColor = BorderColor
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("الرصيد الفعلي الحالي:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("${formatNumber(tank.currentStockLiters)} لتر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = DarkSlate)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("السعة القصوى:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text("${formatNumber(tank.capacityLiters)} لتر (${(percentage * 100).toInt()}%)", fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = BorderColor)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حد التنبيه المنخفض: ${formatNumber(tank.minAlertLiters)}L", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                Text("الحد الحرج: ${formatNumber(tank.criticalAlertLiters)}L", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                            }
                        }
                    }
                }
            }
        } else {
            // Purchases & Fuel Deliveries
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (purchases.isEmpty()) {
                    item {
                        EmptyState(
                            title = "لا توجد توريدات مسجلة",
                            description = "سجل شحنات وتوريدات الوقود المستلمة من الموردين لزيادة رصيد الخزانات وتحديث حساب المورد."
                        )
                    }
                } else {
                    items(purchases) { pur ->
                        val supplier = suppliers.firstOrNull { it.id == pur.supplierId }
                        val fuel = fuelTypes.firstOrNull { it.id == pur.fuelTypeId }

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
                                        Text(
                                            text = "فاتورة: ${pur.invoiceNumber}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "المورد: ${supplier?.name ?: "مورد معتمد"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }

                                    Text(
                                        text = formatCurrency(pur.totalAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = BlueAccent
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    color = BackgroundLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${fuel?.nameArabic ?: ""} • ${formatNumber(pur.quantityLiters)} لتر", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        Text("سعر الشراء: ${formatCurrency(pur.pricePerLiter)} / لتر", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val methodLabel = when (pur.paymentMethod) {
                                        "CREDIT" -> "آجل (التزام على المحطة)"
                                        "CASH" -> "نقداً (الخزينة)"
                                        "ACCOUNT" -> "حساب بنكي"
                                        else -> pur.paymentMethod
                                    }
                                    Text("طريقة الدفع: $methodLabel", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("تاريخ: ${pur.purchaseDate}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
