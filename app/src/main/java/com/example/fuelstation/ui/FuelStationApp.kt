package com.example.fuelstation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fuelstation.data.local.entities.*
import com.example.fuelstation.ui.components.*
import com.example.fuelstation.ui.screens.*
import com.example.ui.theme.*

enum class AppNavTab(val title: String, val icon: ImageVector) {
    DASHBOARD("الرئيسية", Icons.Default.Home),
    OPERATIONAL_DAY("اليوم", Icons.Default.Today),
    READINGS_SALES("المبيعات", Icons.Default.LocalGasStation),
    INVENTORY("المخزون", Icons.Default.Inventory2),
    MORE("المزيد", Icons.Default.Menu)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelStationApp(
    viewModel: FuelStationViewModel = viewModel()
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val userFeedback by viewModel.userFeedback.collectAsStateWithLifecycle()
    val isErrorFeedback by viewModel.isError.collectAsStateWithLifecycle()

    val station by viewModel.station.collectAsStateWithLifecycle()
    val activeDay by viewModel.activeDay.collectAsStateWithLifecycle()
    val allDays by viewModel.allDays.collectAsStateWithLifecycle()
    val fuelTypes by viewModel.fuelTypes.collectAsStateWithLifecycle()
    val pumps by viewModel.pumps.collectAsStateWithLifecycle()
    val nozzles by viewModel.nozzles.collectAsStateWithLifecycle()
    val tanks by viewModel.tanks.collectAsStateWithLifecycle()
    val cashbox by viewModel.cashbox.collectAsStateWithLifecycle()
    val financialAccounts by viewModel.financialAccounts.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val expenseCategories by viewModel.expenseCategories.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

    val activeDayReadings by viewModel.activeDayReadings.collectAsStateWithLifecycle()
    val activeDaySales by viewModel.activeDaySales.collectAsStateWithLifecycle()
    val activeDayExpenses by viewModel.activeDayExpenses.collectAsStateWithLifecycle()
    val activeDayPurchases by viewModel.activeDayPurchases.collectAsStateWithLifecycle()
    val activeDayCashTx by viewModel.activeDayCashTx.collectAsStateWithLifecycle()
    val activeDayVariances by viewModel.activeDayVariances.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppNavTab.DASHBOARD) }

    // Dialogs Visibility State
    var showQuickActionSheet by remember { mutableStateOf(false) }
    var showRecordReadingDialog by remember { mutableStateOf(false) }
    var showAddPurchaseDialog by remember { mutableStateOf(false) }
    var showRecordExpenseDialog by remember { mutableStateOf(false) }
    var showCashMovementDialog by remember { mutableStateOf(false) }
    var showCloseDayDialog by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showAiAssistant by remember { mutableStateOf(false) }
    var supplierToOpenDirectly by remember { mutableStateOf<SupplierEntity?>(null) }
    var selectedFuelForPriceUpdate by remember { mutableStateOf<FuelTypeEntity?>(null) }

    if (showAiAssistant) {
        AiAssistantScreen(
            station = station,
            tanks = tanks,
            fuelTypes = fuelTypes,
            cashbox = cashbox,
            suppliers = suppliers,
            customers = customers,
            onBack = { showAiAssistant = false }
        )
        return
    }

    if (!authState.isLoggedIn) {
        LoginScreen(
            authState = authState,
            onLogin = { username, password, isOffline ->
                viewModel.login(username, password, isOffline)
            }
        )
        return
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = station?.name ?: "نظام إدارة محطة الوقود",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (activeDay?.status == "OPEN") "اليوم التشغيلي مفتوح" else "اليوم مغلق",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeDay?.status == "OPEN") Color(0xFFA7F3D0) else Color(0xFFFDE68A)
                                )
                                if (authState.isOfflineMode) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("• بدون نت", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBAE6FD))
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SurfaceWhite.copy(alpha = 0.2f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountCircle, contentDescription = null, tint = SurfaceWhite, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = authState.currentUser?.fullName ?: "المدير",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SurfaceWhite,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = { showAiAssistant = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = SurfaceWhite.copy(alpha = 0.2f),
                            contentColor = SurfaceWhite
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "المساعد الذكي Gemini")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = SurfaceWhite
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceWhite,
                tonalElevation = 6.dp
            ) {
                AppNavTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                tint = if (isSelected) Primary else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Primary else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showQuickActionSheet = true },
                containerColor = Primary,
                contentColor = SurfaceWhite,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "تسجيل جديد")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تسجيل +", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Container
            when (currentTab) {
                AppNavTab.DASHBOARD -> DashboardScreen(
                    station = station,
                    activeDay = activeDay,
                    tanks = tanks,
                    fuelTypes = fuelTypes,
                    cashbox = cashbox,
                    sales = activeDaySales,
                    expenses = activeDayExpenses,
                    customers = customers,
                    pumps = pumps,
                    nozzles = nozzles,
                    onOpenDayClick = { viewModel.openOperationalDay("افتتاح اليوم التشغيلي") },
                    onCloseDayClick = { showCloseDayDialog = true },
                    onRecordReadingClick = { showRecordReadingDialog = true },
                    onAddPurchaseClick = { showAddPurchaseDialog = true },
                    onRecordExpenseClick = { showRecordExpenseDialog = true },
                    onCashMovementClick = { showCashMovementDialog = true }
                )

                AppNavTab.OPERATIONAL_DAY -> OperationalDayScreen(
                    activeDay = activeDay,
                    allDays = allDays,
                    sales = activeDaySales,
                    expenses = activeDayExpenses,
                    cashTx = activeDayCashTx,
                    variances = activeDayVariances,
                    cashbox = cashbox,
                    onOpenDayClick = { viewModel.openOperationalDay("افتتاح يوم تشغيلي جديد") },
                    onCloseDayClick = { showCloseDayDialog = true }
                )

                AppNavTab.READINGS_SALES -> ReadingsAndSalesScreen(
                    readings = activeDayReadings,
                    sales = activeDaySales,
                    pumps = pumps,
                    nozzles = nozzles,
                    fuelTypes = fuelTypes,
                    customers = customers,
                    accounts = financialAccounts,
                    onRecordReadingClick = { showRecordReadingDialog = true }
                )

                AppNavTab.INVENTORY -> InventoryScreen(
                    tanks = tanks,
                    fuelTypes = fuelTypes,
                    purchases = activeDayPurchases,
                    suppliers = suppliers,
                    onAddPurchaseClick = { showAddPurchaseDialog = true }
                )

                AppNavTab.MORE -> MoreMenuScreen(
                    station = station,
                    pumps = pumps,
                    nozzles = nozzles,
                    fuelTypes = fuelTypes,
                    cashbox = cashbox,
                    accounts = financialAccounts,
                    expenses = activeDayExpenses,
                    customers = customers,
                    suppliers = suppliers,
                    employees = employees,
                    auditLogs = auditLogs,
                    activeDay = activeDay,
                    allDays = allDays,
                    sales = activeDaySales,
                    tanks = tanks,
                    variances = activeDayVariances,
                    viewModel = viewModel,
                    initialSelectedSupplier = supplierToOpenDirectly,
                    onAddCustomerClick = { showAddCustomerDialog = true },
                    onAddSupplierClick = { showAddSupplierDialog = true },
                    onAddExpenseClick = { showRecordExpenseDialog = true },
                    onCashMovementClick = { showCashMovementDialog = true },
                    onUpdatePriceClick = { selectedFuelForPriceUpdate = it },
                    onSaveSettings = { name, address, phone, currency ->
                        viewModel.updateStationSettings(name, address, phone, currency)
                    },
                    onLogout = { viewModel.logout() }
                )
            }

            // Top Feedback Banner (Arabic message on success / error)
            FeedbackBanner(
                message = userFeedback,
                isError = isErrorFeedback,
                onDismiss = { viewModel.clearFeedback() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    // ----------------------------------------------------
    // Quick Action Speed Dial Bottom Sheet
    // ----------------------------------------------------
    if (showQuickActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionSheet = false },
            containerColor = SurfaceWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "تسجيل عملية جديدة",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = DarkSlate
                )
                Text(
                    text = "اختر نوع العملية التي ترغب بتسجيلها في النظام",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(18.dp))

                SpeedDialActionItem(
                    icon = Icons.Default.Speed,
                    title = "تسجيل قراءة عداد واحتساب مبيعات",
                    subtitle = "إدخال قراءات المسدسات وحساب اللترات والمبالغ",
                    color = Primary,
                    onClick = {
                        showQuickActionSheet = false
                        showRecordReadingDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.LocalShipping,
                    title = "إضافة توريد وقود من مورد",
                    subtitle = "استلام شحنة وقود وزيادة رصيد الخزان",
                    color = BlueAccent,
                    onClick = {
                        showQuickActionSheet = false
                        showAddPurchaseDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.Receipt,
                    title = "تسجيل مصروف تشغيلي",
                    subtitle = "سند صرف لمستفيد وخصم من الخزينة / الحساب",
                    color = WarningAmber,
                    onClick = {
                        showQuickActionSheet = false
                        showRecordExpenseDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.Payments,
                    title = "إيداع أو سحب نقدي (الخزينة)",
                    subtitle = "حركات السيولة النقدية اليدوية في الصندوق",
                    color = Color(0xFF0D9488),
                    onClick = {
                        showQuickActionSheet = false
                        showCashMovementDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.PersonAdd,
                    title = "إضافة عميل جديد (ذمم آجلة)",
                    subtitle = "فتح حساب عميل للبيع الآجل",
                    color = Color(0xFF8B5CF6),
                    onClick = {
                        showQuickActionSheet = false
                        showAddCustomerDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.Business,
                    title = "إضافة مورد وقود جديد",
                    subtitle = "تسجيل بيانات شركة أو مورد مشتقات",
                    color = Color(0xFFE11D48),
                    onClick = {
                        showQuickActionSheet = false
                        showAddSupplierDialog = true
                    }
                )

                SpeedDialActionItem(
                    icon = Icons.Default.SmartToy,
                    title = "المساعد الذكي لإطاعة الأوامر (Gemini)",
                    subtitle = "أوامر صوتية حية، استعلامات فورية، وتفريغ صوتي",
                    color = Primary,
                    onClick = {
                        showQuickActionSheet = false
                        showAiAssistant = true
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ----------------------------------------------------
    // Dialog Triggers
    // ----------------------------------------------------
    if (showRecordReadingDialog) {
        RecordReadingDialog(
            nozzles = nozzles,
            pumps = pumps,
            fuelTypes = fuelTypes,
            customers = customers,
            accounts = financialAccounts,
            employees = employees,
            onDismiss = { showRecordReadingDialog = false },
            onConfirm = { nozzleId, currentReading, paymentMethod, customerId, financialAccountId, employeeId, notes ->
                viewModel.recordReadingAndSale(nozzleId, currentReading, paymentMethod, customerId, financialAccountId, employeeId, notes)
                showRecordReadingDialog = false
            }
        )
    }

    if (showAddPurchaseDialog) {
        AddPurchaseDialog(
            suppliers = suppliers,
            fuelTypes = fuelTypes,
            tanks = tanks,
            accounts = financialAccounts,
            onDismiss = { showAddPurchaseDialog = false },
            onConfirm = { supplierId, fuelTypeId, tankId, quantityLiters, pricePerLiter, invoiceNumber, paymentMethod, financialAccountId, notes ->
                viewModel.recordPurchase(supplierId, fuelTypeId, tankId, quantityLiters, pricePerLiter, invoiceNumber, paymentMethod, financialAccountId, notes)
                showAddPurchaseDialog = false
            }
        )
    }

    if (showRecordExpenseDialog) {
        RecordExpenseDialog(
            categories = expenseCategories,
            accounts = financialAccounts,
            onDismiss = { showRecordExpenseDialog = false },
            onConfirm = { categoryId, amount, beneficiary, description, paymentMethod, financialAccountId, voucherNumber, notes ->
                viewModel.recordExpense(categoryId, amount, beneficiary, description, paymentMethod, financialAccountId, voucherNumber, notes)
                showRecordExpenseDialog = false
            }
        )
    }

    if (showCashMovementDialog) {
        CashMovementDialog(
            initialIsCashIn = true,
            onDismiss = { showCashMovementDialog = false },
            onConfirm = { isCashIn, amount, reason, beneficiary, notes ->
                viewModel.recordCashMovement(isCashIn, amount, reason, beneficiary, notes)
                showCashMovementDialog = false
            }
        )
    }

    if (showCloseDayDialog && activeDay != null) {
        CloseDayDialog(
            day = activeDay!!,
            currentCashboxBalance = cashbox?.currentBalance ?: 0.0,
            onDismiss = { showCloseDayDialog = false },
            onConfirm = { actualCash, notes ->
                viewModel.closeOperationalDay(activeDay!!.id, actualCash, notes)
                showCloseDayDialog = false
            }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, address, creditLimit, notes ->
                viewModel.addCustomer(name, phone, address, creditLimit, notes)
                showAddCustomerDialog = false
            }
        )
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onDismiss = { showAddSupplierDialog = false },
            onConfirm = { name, phone, address, notes ->
                viewModel.addSupplier(name, phone, address, notes) { newId ->
                    showAddSupplierDialog = false
                    supplierToOpenDirectly = SupplierEntity(
                        id = newId,
                        name = name,
                        phone = phone,
                        address = address,
                        notes = notes
                    )
                    currentTab = AppNavTab.MORE
                }
            }
        )
    }

    if (selectedFuelForPriceUpdate != null) {
        UpdateFuelPriceDialog(
            fuelType = selectedFuelForPriceUpdate!!,
            onDismiss = { selectedFuelForPriceUpdate = null },
            onConfirm = { fuelTypeId, newPrice, notes ->
                viewModel.updateFuelPrice(fuelTypeId, newPrice, notes)
                selectedFuelForPriceUpdate = null
            }
        )
    }
}

@Composable
fun SpeedDialActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = BackgroundLight,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = DarkSlate)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
        }
    }
}
