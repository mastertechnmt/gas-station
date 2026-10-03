package com.example.fuelstation.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.fuelstation.data.local.entities.SupplierEntity
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddSupplierTransactionDialog(
    supplier: SupplierEntity,
    onDismiss: () -> Unit,
    onSaveTransaction: (
        amount: Double,
        transactionType: String, // "supplier_due" or "station_due"
        currency: String,
        transactionDate: String,
        details: String,
        attachments: String,
        saveAndOpenAnother: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()) }

    var amountText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<String?>(null) } // "supplier_due" (عليه) or "station_due" (له)
    var selectedCurrency by remember { mutableStateOf("ريال يمني (YER)") }
    var currencyExpanded by remember { mutableStateOf(false) }
    var transactionDate by remember { mutableStateOf(todayDateStr) }
    var detailsText by remember { mutableStateOf("") }
    var attachmentPath by remember { mutableStateOf<String?>(null) }
    var showCalculator by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "supp_att_${System.currentTimeMillis()}.jpg"
                val destFile = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(destFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                attachmentPath = destFile.absolutePath
            } catch (e: Exception) {
                attachmentPath = uri.toString()
            }
        }
    }

    fun validateAndSubmit(openAnother: Boolean) {
        val amount = amountText.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            errorMessage = "يرجى إدخال مبلغ صحيح أكبر من صفر"
            return
        }
        if (selectedType == null) {
            errorMessage = "يرجى تحديد نوع العملية: (له أو عليه)"
            return
        }
        if (transactionDate.isBlank()) {
            errorMessage = "يرجى إدخال تاريخ العملية"
            return
        }

        errorMessage = null
        val cleanCurrency = if (selectedCurrency.contains("YER")) "YER" else if (selectedCurrency.contains("SAR")) "SAR" else "USD"

        onSaveTransaction(
            amount,
            selectedType!!,
            cleanCurrency,
            transactionDate,
            detailsText,
            attachmentPath ?: "",
            openAnother
        )

        if (openAnother) {
            // Reset fields for the next transaction
            amountText = ""
            selectedType = null
            detailsText = ""
            attachmentPath = null
            transactionDate = todayDateStr
            errorMessage = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: Supplier Name & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "إضافة عملية للمورد",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = supplier.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إلغاء", tint = TextSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Amount Field with Calculator Button
                    Column {
                        Text(
                            text = "المبلغ *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = amountText,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) {
                                        amountText = it
                                        errorMessage = null
                                    }
                                },
                                placeholder = { Text("0.00") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Primary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    unfocusedBorderColor = BorderColor,
                                    focusedContainerColor = BackgroundLight,
                                    unfocusedContainerColor = BackgroundLight
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            // Calculator Button
                            FilledTonalButton(
                                onClick = { showCalculator = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Primary.copy(alpha = 0.12f),
                                    contentColor = Primary
                                ),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Text(
                                    text = "🧮 حاسبة",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    // 2. Transaction Type Selection: له / عليه
                    Column {
                        Text(
                            text = "نوع العملية *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // [ عليه ] Card (مستحق للمورد على المحطة)
                            val isAlaih = selectedType == "supplier_due"
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlaih) Color(0xFFFEE2E2) else SurfaceWhite
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isAlaih) Color(0xFFDC2626) else BorderColor
                                    ),
                                    width = if (isAlaih) 2.dp else 1.dp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedType = "supplier_due"
                                        errorMessage = null
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "عليه",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAlaih) Color(0xFFDC2626) else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "مستحق للمورد على المحطة (مثل شراء وقود)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isAlaih) Color(0xFF991B1B) else TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }

                            // [ له ] Card (لصالح المحطة عند المورد)
                            val isLahu = selectedType == "station_due"
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLahu) Color(0xFFDCFCE7) else SurfaceWhite
                                ),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        if (isLahu) Color(0xFF16A34A) else BorderColor
                                    ),
                                    width = if (isLahu) 2.dp else 1.dp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedType = "station_due"
                                        errorMessage = null
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "له",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLahu) Color(0xFF16A34A) else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "لصالح المحطة عند المورد (سداد أو دفعة مقدمة)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isLahu) Color(0xFF166534) else TextSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // 3. Currency Selector & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Currency
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = "العملة",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedCard(
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { currencyExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedCurrency, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false }
                                ) {
                                    listOf("ريال يمني (YER)", "ريال سعودي (SAR)", "دولار أمريكي (USD)").forEach { curr ->
                                        DropdownMenuItem(
                                            text = { Text(curr, fontWeight = FontWeight.Medium) },
                                            onClick = {
                                                selectedCurrency = curr
                                                currencyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Date
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التاريخ",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = transactionDate,
                                onValueChange = { transactionDate = it },
                                singleLine = true,
                                trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // 4. Details (Multi-line)
                    Column {
                        Text(
                            text = "التفاصيل (اختياري)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = detailsText,
                            onValueChange = { detailsText = it },
                            placeholder = { Text("مثال: شراء ديزل، دفعة للمورد، تسوية حساب، مرتجع، دفعة مقدمة...") },
                            minLines = 3,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 5. Attachments Section
                    Column {
                        Text(
                            text = "المرفق (سند، فاتورة، إشعار)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        if (attachmentPath != null) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AsyncImage(
                                            model = attachmentPath,
                                            contentDescription = "معاينة المرفق",
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Column {
                                            Text("تم إرفاق صورة المستند", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                            Text("جاهزة للحفظ مع العملية", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                        }
                                    }
                                    IconButton(onClick = { attachmentPath = null }) {
                                        Icon(Icons.Default.Delete, contentDescription = "حذف الصورة", tint = ErrorRed)
                                    }
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("📷 إضافة صورة / سند")
                            }
                        }
                    }

                    // Error Message Banner
                    if (errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(errorMessage!!, style = MaterialTheme.typography.bodySmall, color = ErrorRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BorderColor)

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // [ حفظ وفتح عملية أخرى ]
                    OutlinedButton(
                        onClick = { validateAndSubmit(openAnother = true) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وفتح عملية أخرى", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }

                    // [ حفظ وإغلاق ]
                    Button(
                        onClick = { validateAndSubmit(openAnother = false) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ وإغلاق", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCalculator) {
        FinancialCalculatorDialog(
            initialValue = amountText,
            onDismiss = { showCalculator = false },
            onConfirmResult = { calculatedResult ->
                amountText = calculatedResult
                errorMessage = null
            }
        )
    }
}
