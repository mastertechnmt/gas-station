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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReadingsAndSalesScreen(
    readings: List<ReadingEntity>,
    sales: List<SaleEntity>,
    pumps: List<PumpEntity>,
    nozzles: List<NozzleEntity>,
    fuelTypes: List<FuelTypeEntity>,
    customers: List<CustomerEntity>,
    accounts: List<FinancialAccountEntity>,
    onRecordReadingClick: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: قراءات العدادات, 1: سجل المبيعات
    var selectedFuelFilter by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredReadings = readings.filter { r ->
        (selectedFuelFilter == null || r.fuelTypeId == selectedFuelFilter)
    }

    val filteredSales = sales.filter { s ->
        (selectedFuelFilter == null || s.fuelTypeId == selectedFuelFilter)
    }

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
                text = { Text("قراءات العدادات (${filteredReadings.size})", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("سجل المبيعات (${filteredSales.size})", fontWeight = FontWeight.Bold) }
            )
        }

        // Action & Filter Bar
        Surface(
            color = SurfaceWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRecordReadingClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل قراءة عداد جديدة", fontWeight = FontWeight.Bold)
                    }

                    // Fuel Filter Chips
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = selectedFuelFilter == null,
                            onClick = { selectedFuelFilter = null },
                            label = { Text("الكل", style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                        fuelTypes.forEach { f ->
                            FilterChip(
                                selected = selectedFuelFilter == f.id,
                                onClick = { selectedFuelFilter = if (selectedFuelFilter == f.id) null else f.id },
                                label = { Text(f.code, style = MaterialTheme.typography.labelSmall) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            // Readings List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredReadings.isEmpty()) {
                    item {
                        EmptyState(
                            title = "لا توجد قراءات مسجلة",
                            description = "سجل أول قراءة للمسدسات بالضغط على زر 'تسجيل قراءة عداد جديدة' أعلاه."
                        )
                    }
                } else {
                    items(filteredReadings) { reading ->
                        val nozzle = nozzles.firstOrNull { it.id == reading.nozzleId }
                        val pump = pumps.firstOrNull { it.id == reading.pumpId }
                        val fuel = fuelTypes.firstOrNull { it.id == reading.fuelTypeId }

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${pump?.name ?: "طرمبة"} - مسدس #${nozzle?.nozzleNumber ?: 1}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        if (fuel != null) {
                                            FuelTypeBadge(name = fuel.nameArabic, code = fuel.code, colorHex = fuel.colorHex)
                                        }
                                    }
                                    val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                                    Text(
                                        text = sdf.format(Date(reading.recordedAt)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Reading Values Row
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
                                        Column {
                                            Text("القراءة السابقة:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text(formatNumber(reading.previousReading), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextMuted, modifier = Modifier.align(Alignment.CenterVertically))
                                        Column {
                                            Text("القراءة الحالية:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text(formatNumber(reading.currentReading), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = Primary)
                                        }
                                        Column {
                                            Text("الكمية المباعة:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            Text("${formatNumber(reading.soldLiters)} لتر", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = DarkSlate)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "السعر: ${formatCurrency(reading.pricePerLiter)} / لتر",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "الإجمالي: ${formatCurrency(reading.totalAmount)}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Sales List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 14.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredSales.isEmpty()) {
                    item {
                        EmptyState(
                            title = "لا توجد مبيعات مسجلة",
                            description = "تسجيل قراءات العدادات يولد عمليات البيع تلقائياً."
                        )
                    }
                } else {
                    items(filteredSales) { sale ->
                        val fuel = fuelTypes.firstOrNull { it.id == sale.fuelTypeId }
                        val customer = customers.firstOrNull { it.id == sale.customerId }
                        val account = accounts.firstOrNull { it.id == sale.financialAccountId }

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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "مبيعات ${fuel?.nameArabic ?: "وقود"}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val paymentBadgeColor = when (sale.paymentMethod) {
                                            "CASH" -> SuccessGreen
                                            "ACCOUNT" -> BlueAccent
                                            "CREDIT" -> Color(0xFF8B5CF6)
                                            else -> Primary
                                        }
                                        val paymentLabel = when (sale.paymentMethod) {
                                            "CASH" -> "نقداً (الخزينة)"
                                            "ACCOUNT" -> "حساب / محفظة"
                                            "CREDIT" -> "آجل (ذمم)"
                                            else -> sale.paymentMethod
                                        }
                                        StatusBadge(
                                            text = paymentLabel,
                                            backgroundColor = paymentBadgeColor.copy(alpha = 0.15f),
                                            contentColor = paymentBadgeColor
                                        )
                                    }

                                    Text(
                                        text = formatCurrency(sale.totalAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "الكمية: ${formatNumber(sale.quantityLiters)} لتر × ${formatCurrency(sale.unitPrice)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )

                                    if (sale.paymentMethod == "CREDIT" && customer != null) {
                                        Text(
                                            text = "العميل: ${customer.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF8B5CF6)
                                        )
                                    } else if (sale.paymentMethod == "ACCOUNT" && account != null) {
                                        Text(
                                            text = "الحساب: ${account.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BlueAccent
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
