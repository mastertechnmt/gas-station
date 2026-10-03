package com.example.fuelstation.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.FuelStationViewModel
import com.example.fuelstation.ui.components.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import java.text.SimpleDateFormat
import java.util.*

enum class MoreSubScreen {
    MENU,
    REPORTS,
    FINANCE,
    EXPENSES,
    PUMPS,
    FUELS,
    CUSTOMERS,
    SUPPLIERS,
    EMPLOYEES,
    AUDIT_LOG,
    SETTINGS
}

@Composable
fun MoreMenuScreen(
    station: StationEntity?,
    pumps: List<PumpEntity>,
    nozzles: List<NozzleEntity>,
    fuelTypes: List<FuelTypeEntity>,
    cashbox: CashboxEntity?,
    accounts: List<FinancialAccountEntity>,
    expenses: List<ExpenseEntity>,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    employees: List<EmployeeEntity>,
    auditLogs: List<AuditLogEntity>,
    activeDay: OperationalDayEntity? = null,
    allDays: List<OperationalDayEntity> = emptyList(),
    sales: List<SaleEntity> = emptyList(),
    tanks: List<TankEntity> = emptyList(),
    variances: List<VarianceEntity> = emptyList(),
    viewModel: FuelStationViewModel? = null,
    initialSelectedSupplier: SupplierEntity? = null,
    onAddCustomerClick: () -> Unit,
    onAddSupplierClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onCashMovementClick: () -> Unit,
    onUpdatePriceClick: (FuelTypeEntity) -> Unit,
    onSaveSettings: (name: String, address: String, phone: String, currency: String) -> Unit,
    onLogout: () -> Unit
) {
    var currentSubScreen by remember { mutableStateOf(if (initialSelectedSupplier != null) MoreSubScreen.SUPPLIERS else MoreSubScreen.MENU) }
    var selectedSupplierForAccount by remember { mutableStateOf<SupplierEntity?>(initialSelectedSupplier) }
    var showAddSupplierDialogLocal by remember { mutableStateOf(false) }
    var showAddTransactionDialogLocal by remember { mutableStateOf(false) }

    LaunchedEffect(initialSelectedSupplier) {
        if (initialSelectedSupplier != null) {
            currentSubScreen = MoreSubScreen.SUPPLIERS
            selectedSupplierForAccount = initialSelectedSupplier
        }
    }

    BackHandler(enabled = currentSubScreen != MoreSubScreen.MENU) {
        if (selectedSupplierForAccount != null) {
            selectedSupplierForAccount = null
        } else {
            currentSubScreen = MoreSubScreen.MENU
        }
    }

    when (currentSubScreen) {
        MoreSubScreen.MENU -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header profile card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = Primary, modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text("مدير المحطة (حساب النظام)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${station?.name ?: "محطة الوقود"} • بصلاحيات كاملة", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                item {
                    Text("الإدارات والعمليات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMuted)
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Assessment,
                        title = "التقارير والمطابقات الشاملة",
                        subtitle = "التقرير اليومي، المبيعات، حركة الخزانات، الفروقات",
                        iconColor = Primary,
                        onClick = { currentSubScreen = MoreSubScreen.REPORTS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.AccountBalance,
                        title = "الخزينة والحسابات المالية والمحافظ",
                        subtitle = "رصيد الخزينة: ${formatCurrency(cashbox?.currentBalance ?: 0.0)}",
                        iconColor = BlueAccent,
                        onClick = { currentSubScreen = MoreSubScreen.FINANCE }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.ReceiptLong,
                        title = "المصروفات التشغيلية والسندات",
                        subtitle = "${expenses.size} سندات مصروفات مسجلة",
                        iconColor = WarningAmber,
                        onClick = { currentSubScreen = MoreSubScreen.EXPENSES }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Speed,
                        title = "الطرمبات والمسدسات",
                        subtitle = "${pumps.size} طرمبات • ${nozzles.size} مسدس",
                        iconColor = Primary,
                        onClick = { currentSubScreen = MoreSubScreen.PUMPS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.LocalGasStation,
                        title = "أنواع وأسعار الوقود التاريخية",
                        subtitle = "${fuelTypes.size} مشتقات نفطية معتمدة",
                        iconColor = Color(0xFF0D9488),
                        onClick = { currentSubScreen = MoreSubScreen.FUELS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.People,
                        title = "العملاء والمبيعات الآجلة (الذمم)",
                        subtitle = "${customers.size} عملاء • ديون: ${formatCurrency(customers.sumOf { it.currentBalance })}",
                        iconColor = Color(0xFF8B5CF6),
                        onClick = { currentSubScreen = MoreSubScreen.CUSTOMERS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Business,
                        title = "الموردون وسجلات التوريد (الالتزامات)",
                        subtitle = "${suppliers.size} موردين • مستحقات: ${formatCurrency(suppliers.sumOf { it.currentBalance })}",
                        iconColor = Color(0xFFE11D48),
                        onClick = { currentSubScreen = MoreSubScreen.SUPPLIERS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Badge,
                        title = "الموظفون وعمال الطرمبات",
                        subtitle = "${employees.size} موظفين وعمال مسجلين",
                        iconColor = Color(0xFFD97706),
                        onClick = { currentSubScreen = MoreSubScreen.EMPLOYEES }
                    )
                }

                item {
                    Text("النظام والرقابة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextMuted)
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Security,
                        title = "سجل التدقيق والرقابة (Audit Log)",
                        subtitle = "توثيق العمليات الحساسة والتعديلات (${auditLogs.size})",
                        iconColor = DarkSlate,
                        onClick = { currentSubScreen = MoreSubScreen.AUDIT_LOG }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Settings,
                        title = "إعدادات المحطة والنظام",
                        subtitle = "الاسم، العنوان، الهاتف، العملة",
                        iconColor = Color(0xFF475569),
                        onClick = { currentSubScreen = MoreSubScreen.SETTINGS }
                    )
                }

                item {
                    MoreMenuItem(
                        icon = Icons.Default.Logout,
                        title = "تسجيل الخروج",
                        subtitle = "إنهاء الجلسة والعودة لشاشة الدخول",
                        iconColor = ErrorRed,
                        onClick = onLogout
                    )
                }
            }
        }

        // Sub Screens with Back Bar
        MoreSubScreen.REPORTS -> {
            SubScreenContainer(title = "التقارير والمطابقات المالية والتشغيلية", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                ReportsScreen(
                    station = station,
                    activeDay = activeDay,
                    allDays = allDays,
                    sales = sales,
                    expenses = expenses,
                    fuelTypes = fuelTypes,
                    tanks = tanks,
                    customers = customers,
                    suppliers = suppliers,
                    cashbox = cashbox,
                    variances = variances
                )
            }
        }

        MoreSubScreen.FINANCE -> {
            SubScreenContainer(title = "الخزينة والحسابات المالية", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        StatCard(
                            title = "رصيد الخزينة الحالي",
                            value = formatCurrency(cashbox?.currentBalance ?: 0.0),
                            subtitle = "النقدية الفعلية داخل الصندوق",
                            icon = Icons.Default.AccountBalanceWallet,
                            iconTint = Primary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Button(
                            onClick = onCashMovementClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل حركة نقدية (إيداع / سحب)")
                        }
                    }

                    item {
                        Text("الحسابات المالية والمحافظ الإلكترونية", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    items(accounts) { acc ->
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
                                    Text(acc.name, fontWeight = FontWeight.Bold)
                                    Text("نوع الحساب: ${acc.type} • رقم: ${acc.accountNumber}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(formatCurrency(acc.currentBalance), fontWeight = FontWeight.Bold, color = BlueAccent)
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.EXPENSES -> {
            SubScreenContainer(title = "سجل المصروفات التشغيلية", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Button(
                            onClick = onAddExpenseClick,
                            colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = DarkSlate)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل سند مصروف جديد", color = DarkSlate, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (expenses.isEmpty()) {
                        item { EmptyState(title = "لا توجد مصروفات", description = "سجل المصروفات التشغيلية هنا") }
                    } else {
                        items(expenses) { exp ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(exp.beneficiary, fontWeight = FontWeight.Bold)
                                        Text(formatCurrency(exp.amount), fontWeight = FontWeight.Bold, color = ErrorRed)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(exp.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("طريقة الصرف: ${exp.paymentMethod} • سند رقم: ${exp.voucherNumber}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.PUMPS -> {
            SubScreenContainer(title = "الطرمبات والمسدسات", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(pumps) { pump ->
                        val pumpNozzles = nozzles.filter { it.pumpId == pump.id }
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
                                    Text(pump.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    StatusBadge(text = "نشط", backgroundColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("المسدسات المرتبطة بهذه الطرمبة:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                pumpNozzles.forEach { n ->
                                    val fuel = fuelTypes.firstOrNull { it.id == n.fuelTypeId }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("مسدس #${n.nozzleNumber} (${fuel?.nameArabic ?: ""})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                        Text("آخر قراءة: ${formatNumber(n.lastReading)}", style = MaterialTheme.typography.bodySmall, color = Primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.FUELS -> {
            SubScreenContainer(title = "أنواع وأسعار الوقود", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(fuelTypes) { fuel ->
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
                                    FuelTypeBadge(name = fuel.nameArabic, code = fuel.code, colorHex = fuel.colorHex)
                                    Text(formatCurrency(fuel.currentPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Primary)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onUpdatePriceClick(fuel) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Primary.copy(alpha = 0.1f), contentColor = Primary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تحديث السعر وتوثيق التاريخ")
                                }
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.CUSTOMERS -> {
            SubScreenContainer(title = "العملاء والمبيعات الآجلة", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Button(
                            onClick = onAddCustomerClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إضافة عميل جديد")
                        }
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
                                    Text("الرصيد الآجل المستحق:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text(formatCurrency(c.currentBalance), fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                }
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.SUPPLIERS -> {
            if (selectedSupplierForAccount != null) {
                val currentSupplier = selectedSupplierForAccount!!
                val transactions by (viewModel?.getSupplierTransactions(currentSupplier.id) ?: flowOf(emptyList()))
                    .collectAsState(initial = emptyList())
                val summary = remember(transactions) {
                    viewModel?.computeSupplierSummary(transactions)
                        ?: com.example.fuelstation.data.repository.SupplierBalanceSummary()
                }

                SupplierAccountScreen(
                    supplier = currentSupplier,
                    transactions = transactions,
                    summary = summary,
                    onBack = { selectedSupplierForAccount = null },
                    onAddTransactionClick = { showAddTransactionDialogLocal = true },
                    onCancelTransaction = { txId, reason ->
                        viewModel?.cancelSupplierTransaction(txId, reason)
                    }
                )

                if (showAddTransactionDialogLocal) {
                    AddSupplierTransactionDialog(
                        supplier = currentSupplier,
                        onDismiss = { showAddTransactionDialogLocal = false },
                        onSaveTransaction = { amount, type, currency, date, details, attachments, saveAndOpenAnother ->
                            viewModel?.recordSupplierLedgerTransaction(
                                supplierId = currentSupplier.id,
                                amount = amount,
                                transactionType = type,
                                currency = currency,
                                transactionDate = date,
                                details = details,
                                attachments = attachments
                            ) {
                                if (!saveAndOpenAnother) {
                                    showAddTransactionDialogLocal = false
                                }
                            }
                        }
                    )
                }
            } else {
                SubScreenContainer(title = "الموردون وسجلات التوريد", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Button(
                                onClick = { showAddSupplierDialogLocal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("➕ إضافة مورد", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        items(suppliers) { sup ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSupplierForAccount = sup }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Primary.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Business, contentDescription = null, tint = Primary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(sup.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("هاتف: ${sup.phone.ifBlank { "غير مسجل" }}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("الرصيد:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                            val isSupplierDue = sup.currentBalance >= 0
                                            val balColor = if (isSupplierDue) Color(0xFFDC2626) else Color(0xFF16A34A)
                                            val balStatus = if (sup.currentBalance > 0) "على المحطة" else if (sup.currentBalance < 0) "للمحطة" else "متسوي"
                                            Text(
                                                "${formatNumber(kotlin.math.abs(sup.currentBalance))} ر.ي",
                                                fontWeight = FontWeight.Bold,
                                                color = balColor
                                            )
                                            Text(balStatus, style = MaterialTheme.typography.labelSmall, color = balColor)
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (showAddSupplierDialogLocal) {
                    AddSupplierDialog(
                        onDismiss = { showAddSupplierDialogLocal = false },
                        onConfirm = { name, phone, address, notes ->
                            viewModel?.addSupplier(name, phone, address, notes) { newId ->
                                showAddSupplierDialogLocal = false
                                selectedSupplierForAccount = SupplierEntity(
                                    id = newId,
                                    name = name,
                                    phone = phone,
                                    address = address,
                                    notes = notes
                                )
                            }
                        }
                    )
                }
            }
        }

        MoreSubScreen.EMPLOYEES -> {
            SubScreenContainer(title = "الموظفون والعمال", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(employees) { emp ->
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
                                    Text(emp.name, fontWeight = FontWeight.Bold)
                                    Text(emp.jobTitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Text("هاتف: ${emp.phone}", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                StatusBadge(text = "على رأس العمل", backgroundColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.AUDIT_LOG -> {
            SubScreenContainer(title = "سجل التدقيق والرقابة (Audit Log)", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(auditLogs) { log ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.action, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Primary)
                                    Text(sdf.format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(log.details, style = MaterialTheme.typography.bodySmall, color = DarkSlate)
                                Text("المستخدم: ${log.username} • الكيان: ${log.entityName}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        MoreSubScreen.SETTINGS -> {
            SubScreenContainer(title = "إعدادات المحطة والنظام", onBack = { currentSubScreen = MoreSubScreen.MENU }) {
                var stationName by remember { mutableStateOf(station?.name ?: "محطة الوقود الرئيسية") }
                var stationAddress by remember { mutableStateOf(station?.address ?: "صنعاء - الجمهورية اليمنية") }
                var stationPhone by remember { mutableStateOf(station?.phone ?: "+967 770 000 000") }
                var stationCurrency by remember { mutableStateOf(station?.currency ?: "ريال يمني") }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = stationName,
                        onValueChange = { stationName = it },
                        label = { Text("اسم المحطة") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = stationAddress,
                        onValueChange = { stationAddress = it },
                        label = { Text("العنوان / الموقع") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = stationPhone,
                        onValueChange = { stationPhone = it },
                        label = { Text("رقم الهاتف") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = stationCurrency,
                        onValueChange = { stationCurrency = it },
                        label = { Text("العملة الرسمية") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = { onSaveSettings(stationName, stationAddress, stationPhone, stationCurrency) },
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("حفظ التغييرات")
                    }
                }
            }
        }
    }
}

@Composable
fun SubScreenContainer(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Surface(
            color = SurfaceWhite,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "رجوع", tint = DarkSlate)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DarkSlate)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = TextMuted)
        }
    }
}
