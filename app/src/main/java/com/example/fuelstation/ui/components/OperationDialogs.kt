package com.example.fuelstation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.fuelstation.data.local.entities.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordReadingDialog(
    nozzles: List<NozzleEntity>,
    pumps: List<PumpEntity>,
    fuelTypes: List<FuelTypeEntity>,
    customers: List<CustomerEntity>,
    accounts: List<FinancialAccountEntity>,
    employees: List<EmployeeEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        nozzleId: Long,
        currentReading: Double,
        paymentMethod: String,
        customerId: Long?,
        financialAccountId: Long?,
        employeeId: Long?,
        notes: String
    ) -> Unit
) {
    var selectedNozzleId by remember { mutableStateOf(nozzles.firstOrNull()?.id ?: 0L) }
    val selectedNozzle = nozzles.firstOrNull { it.id == selectedNozzleId }
    val selectedFuel = fuelTypes.firstOrNull { it.id == selectedNozzle?.fuelTypeId }
    val selectedPump = pumps.firstOrNull { it.id == selectedNozzle?.pumpId }

    val previousReading = selectedNozzle?.lastReading ?: 0.0
    var currentReadingText by remember(selectedNozzleId) {
        mutableStateOf(if (previousReading > 0) "${(previousReading + 50).toInt()}" else "100")
    }

    val currentReading = currentReadingText.toDoubleOrNull() ?: 0.0
    val soldLiters = (currentReading - previousReading).coerceAtLeast(0.0)
    val unitPrice = selectedFuel?.currentPrice ?: 0.0
    val totalAmount = soldLiters * unitPrice
    val isReadingInvalid = currentReading < previousReading

    var paymentMethod by remember { mutableStateOf("CASH") } // CASH, ACCOUNT, CREDIT
    var selectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.id) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedEmployeeId by remember { mutableStateOf(employees.firstOrNull()?.id) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل قراءة عداد واحتساب مبيعات", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Select Nozzle
                Text("اختر المسدس / الطرمبة:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))

                var expandedNozzle by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedNozzle,
                    onExpandedChange = { expandedNozzle = !expandedNozzle }
                ) {
                    OutlinedTextField(
                        value = "${selectedPump?.name ?: "طرمبة"} - مسدس #${selectedNozzle?.nozzleNumber ?: 1} (${selectedFuel?.nameArabic ?: ""})",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedNozzle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedNozzle,
                        onDismissRequest = { expandedNozzle = false }
                    ) {
                        nozzles.forEach { n ->
                            val p = pumps.firstOrNull { it.id == n.pumpId }
                            val f = fuelTypes.firstOrNull { it.id == n.fuelTypeId }
                            DropdownMenuItem(
                                text = { Text("${p?.name ?: "طرمبة"} - مسدس #${n.nozzleNumber} (${f?.nameArabic ?: ""})") },
                                onClick = {
                                    selectedNozzleId = n.id
                                    expandedNozzle = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Readings Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = formatNumber(previousReading),
                        onValueChange = {},
                        label = { Text("القراءة السابقة") },
                        readOnly = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BackgroundLight,
                            unfocusedContainerColor = BackgroundLight
                        )
                    )
                    OutlinedTextField(
                        value = currentReadingText,
                        onValueChange = { currentReadingText = it },
                        label = { Text("القراءة الحالية *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        isError = isReadingInvalid,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                if (isReadingInvalid) {
                    Text(
                        text = "خطأ: القراءة الحالية لا يمكن أن تكون أقل من السابقة ($previousReading)!",
                        color = ErrorRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Calculation Summary Card
                Surface(
                    color = Primary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الكمية المباعة:", style = MaterialTheme.typography.bodyMedium)
                            Text("${formatNumber(soldLiters)} لتر", fontWeight = FontWeight.Bold, color = PrimaryDark)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("سعر اللتر:", style = MaterialTheme.typography.bodyMedium)
                            Text(formatCurrency(unitPrice), fontWeight = FontWeight.SemiBold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 6.dp), color = BorderColor)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("إجمالي قيمة المبيعات:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(formatCurrency(totalAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Payment Method
                Text("طريقة الدفع:", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CASH" to "نقداً", "ACCOUNT" to "حساب / محفظة", "CREDIT" to "بيع آجل").forEach { (method, label) ->
                        val selected = paymentMethod == method
                        FilterChip(
                            selected = selected,
                            onClick = { paymentMethod = method },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                if (paymentMethod == "CREDIT") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("اختر العميل الآجل:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    var expandedCust by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCust,
                        onExpandedChange = { expandedCust = !expandedCust }
                    ) {
                        val cust = customers.firstOrNull { it.id == selectedCustomerId }
                        OutlinedTextField(
                            value = cust?.name ?: "اختر العميل",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCust) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCust,
                            onDismissRequest = { expandedCust = false }
                        ) {
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} (رصيد: ${formatCurrency(c.currentBalance)})") },
                                    onClick = {
                                        selectedCustomerId = c.id
                                        expandedCust = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (paymentMethod == "ACCOUNT") {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("اختر الحساب المالي / المحفظة:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    var expandedAcc by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedAcc,
                        onExpandedChange = { expandedAcc = !expandedAcc }
                    ) {
                        val acc = accounts.firstOrNull { it.id == selectedAccountId }
                        OutlinedTextField(
                            value = acc?.name ?: "اختر الحساب",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedAcc,
                            onDismissRequest = { expandedAcc = false }
                        ) {
                            accounts.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text("${a.name} (${formatCurrency(a.currentBalance)})") },
                                    onClick = {
                                        selectedAccountId = a.id
                                        expandedAcc = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedNozzleId,
                        currentReading,
                        paymentMethod,
                        if (paymentMethod == "CREDIT") selectedCustomerId else null,
                        if (paymentMethod == "ACCOUNT") selectedAccountId else null,
                        selectedEmployeeId,
                        notes
                    )
                },
                enabled = !isReadingInvalid && soldLiters > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ واحتساب المبيعات")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPurchaseDialog(
    suppliers: List<SupplierEntity>,
    fuelTypes: List<FuelTypeEntity>,
    tanks: List<TankEntity>,
    accounts: List<FinancialAccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        supplierId: Long,
        fuelTypeId: Long,
        tankId: Long,
        quantityLiters: Double,
        pricePerLiter: Double,
        invoiceNumber: String,
        paymentMethod: String,
        financialAccountId: Long?,
        notes: String
    ) -> Unit
) {
    var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 0L) }
    var selectedFuelTypeId by remember { mutableStateOf(fuelTypes.firstOrNull()?.id ?: 0L) }
    val relevantTanks = tanks.filter { it.fuelTypeId == selectedFuelTypeId }
    var selectedTankId by remember(selectedFuelTypeId) {
        mutableStateOf(relevantTanks.firstOrNull()?.id ?: tanks.firstOrNull()?.id ?: 0L)
    }

    var quantityText by remember { mutableStateOf("10000") }
    val defaultPrice = fuelTypes.firstOrNull { it.id == selectedFuelTypeId }?.currentPrice ?: 380.0
    var priceText by remember(selectedFuelTypeId) { mutableStateOf("${(defaultPrice * 0.95).toInt()}") } // wholesale price
    var invoiceNumber by remember { mutableStateOf("INV-${System.currentTimeMillis() % 100000}") }
    var paymentMethod by remember { mutableStateOf("CREDIT") } // CREDIT, CASH, ACCOUNT
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var notes by remember { mutableStateOf("") }

    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val price = priceText.toDoubleOrNull() ?: 0.0
    val totalAmount = qty * price

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل توريد وقود (شراء)", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Supplier
                Text("المورد:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                var expandedSupp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedSupp, onExpandedChange = { expandedSupp = !expandedSupp }) {
                    val s = suppliers.firstOrNull { it.id == selectedSupplierId }
                    OutlinedTextField(
                        value = s?.name ?: "اختر المورد",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSupp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedSupp, onDismissRequest = { expandedSupp = false }) {
                        suppliers.forEach { sup ->
                            DropdownMenuItem(
                                text = { Text(sup.name) },
                                onClick = {
                                    selectedSupplierId = sup.id
                                    expandedSupp = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Fuel Type
                Text("نوع الوقود:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                var expandedFuel by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedFuel, onExpandedChange = { expandedFuel = !expandedFuel }) {
                    val f = fuelTypes.firstOrNull { it.id == selectedFuelTypeId }
                    OutlinedTextField(
                        value = f?.nameArabic ?: "نوع الوقود",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFuel) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedFuel, onDismissRequest = { expandedFuel = false }) {
                        fuelTypes.forEach { fuel ->
                            DropdownMenuItem(
                                text = { Text(fuel.nameArabic) },
                                onClick = {
                                    selectedFuelTypeId = fuel.id
                                    expandedFuel = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Tank
                Text("الخزان المستهدف للتفريغ:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                var expandedTank by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedTank, onExpandedChange = { expandedTank = !expandedTank }) {
                    val t = tanks.firstOrNull { it.id == selectedTankId }
                    OutlinedTextField(
                        value = t?.name ?: "الخزان",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTank) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedTank, onDismissRequest = { expandedTank = false }) {
                        (if (relevantTanks.isNotEmpty()) relevantTanks else tanks).forEach { tk ->
                            DropdownMenuItem(
                                text = { Text("${tk.name} (المخزون الحالي: ${formatNumber(tk.currentStockLiters)}L)") },
                                onClick = {
                                    selectedTankId = tk.id
                                    expandedTank = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Qty & Price
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("الكمية باللتر *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("سعر الشراء للتر *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = invoiceNumber,
                    onValueChange = { invoiceNumber = it },
                    label = { Text("رقم فاتورة التوريد *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Total Amount Summary
                Surface(
                    color = BlueAccent.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إجمالي الفاتورة:", style = MaterialTheme.typography.titleSmall)
                        Text(formatCurrency(totalAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BlueAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Method
                Text("طريقة السداد للمورد:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("CREDIT" to "آجل (التزام)", "CASH" to "نقداً من الخزينة", "ACCOUNT" to "حساب بنكي").forEach { (method, label) ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedSupplierId,
                        selectedFuelTypeId,
                        selectedTankId,
                        qty,
                        price,
                        invoiceNumber,
                        paymentMethod,
                        if (paymentMethod == "ACCOUNT") selectedAccountId else null,
                        notes
                    )
                },
                enabled = qty > 0 && price > 0 && invoiceNumber.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ التوريد وزيادة المخزون")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordExpenseDialog(
    categories: List<ExpenseCategoryEntity>,
    accounts: List<FinancialAccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (
        categoryId: Long,
        amount: Double,
        beneficiary: String,
        description: String,
        paymentMethod: String,
        financialAccountId: Long?,
        voucherNumber: String,
        notes: String
    ) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var amountText by remember { mutableStateOf("") }
    var beneficiary by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASHBOX") } // CASHBOX, ACCOUNT
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var voucherNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("تسجيل مصروف تشغيلي", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Category
                Text("تصنيف المصروف:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                var expandedCat by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expandedCat, onExpandedChange = { expandedCat = !expandedCat }) {
                    val cat = categories.firstOrNull { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = cat?.nameArabic ?: "اختر التصنيف",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        categories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.nameArabic) },
                                onClick = {
                                    selectedCategoryId = c.id
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (ر.ي) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = beneficiary,
                    onValueChange = { beneficiary = it },
                    label = { Text("المستفيد *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("البيان / الوصف *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text("طريقة الصرف:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = paymentMethod == "CASHBOX",
                        onClick = { paymentMethod = "CASHBOX" },
                        label = { Text("خزينة المحطة") },
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = paymentMethod == "ACCOUNT",
                        onClick = { paymentMethod = "ACCOUNT" },
                        label = { Text("حساب بنكي / محفظة") },
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (paymentMethod == "ACCOUNT") {
                    Spacer(modifier = Modifier.height(8.dp))
                    var expandedAcc by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expandedAcc, onExpandedChange = { expandedAcc = !expandedAcc }) {
                        val acc = accounts.firstOrNull { it.id == selectedAccountId }
                        OutlinedTextField(
                            value = acc?.name ?: "اختر الحساب المالي",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAcc) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenu(expanded = expandedAcc, onDismissRequest = { expandedAcc = false }) {
                            accounts.forEach { a ->
                                DropdownMenuItem(
                                    text = { Text("${a.name} (${formatCurrency(a.currentBalance)})") },
                                    onClick = {
                                        selectedAccountId = a.id
                                        expandedAcc = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        selectedCategoryId,
                        amount,
                        beneficiary,
                        description,
                        paymentMethod,
                        if (paymentMethod == "ACCOUNT") selectedAccountId else null,
                        voucherNumber,
                        notes
                    )
                },
                enabled = amount > 0 && beneficiary.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("حفظ وخصم المصروف")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CashMovementDialog(
    initialIsCashIn: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (isCashIn: Boolean, amount: Double, reason: String, beneficiary: String, notes: String) -> Unit
) {
    var isCashIn by remember { mutableStateOf(initialIsCashIn) }
    var amountText by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var beneficiary by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isCashIn) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                    contentDescription = null,
                    tint = if (isCashIn) SuccessGreen else ErrorRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isCashIn) "إيداع نقدي في الخزينة" else "سحب نقدي من الخزينة", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCashIn,
                        onClick = { isCashIn = true },
                        label = { Text("إيداع نقدي (+)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SuccessGreen.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    )
                    FilterChip(
                        selected = !isCashIn,
                        onClick = { isCashIn = false },
                        label = { Text("سحب نقدي (-)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ErrorRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("المبلغ (ر.ي) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("السبب / البيان *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = beneficiary,
                    onValueChange = { beneficiary = it },
                    label = { Text(if (isCashIn) "المودع (اختياري)" else "المستلم (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(isCashIn, amount, reason, beneficiary, notes) },
                enabled = amount > 0 && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isCashIn) Primary else ErrorRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isCashIn) "تأكيد الإيداع" else "تأكيد السحب")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun CloseDayDialog(
    day: OperationalDayEntity,
    currentCashboxBalance: Double,
    onDismiss: () -> Unit,
    onConfirm: (actualCash: Double, notes: String) -> Unit
) {
    var actualCashText by remember { mutableStateOf("${currentCashboxBalance.toInt()}") }
    var notes by remember { mutableStateOf("") }

    val actualCash = actualCashText.toDoubleOrNull() ?: 0.0
    val expectedCash = currentCashboxBalance
    val diff = actualCash - expectedCash

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LockClock, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("إغلاق ومطابقة اليوم التشغيلي", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "تاريخ اليوم: ${day.dayDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = BackgroundLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("الرصيد الدفتري المتوقع للخزينة:", style = MaterialTheme.typography.bodyMedium)
                            Text(formatCurrency(expectedCash), fontWeight = FontWeight.Bold, color = DarkSlate)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = actualCashText,
                    onValueChange = { actualCashText = it },
                    label = { Text("النقد الفعلي في الخزينة (الجرد) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Variance Card
                val diffColor = when {
                    diff > 0 -> SuccessGreen
                    diff < 0 -> ErrorRed
                    else -> Primary
                }
                Surface(
                    color = diffColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, diffColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("فارق النقدية:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = when {
                                diff > 0 -> "+${formatCurrency(diff)} (فائض)"
                                diff < 0 -> "${formatCurrency(diff)} (عجز)"
                                else -> "مطابقة تامة (0)"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = diffColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إغلاق اليوم") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(actualCash, notes) },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("إغلاق اليوم وتثبيت المطابقة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}
