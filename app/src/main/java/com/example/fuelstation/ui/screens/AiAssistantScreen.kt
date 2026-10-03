package com.example.fuelstation.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fuelstation.data.ai.AudioRecorderHelper
import com.example.fuelstation.data.ai.ChatMessage
import com.example.fuelstation.data.ai.GeminiAiService
import com.example.fuelstation.data.ai.GeminiModel
import com.example.fuelstation.data.local.entities.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AiAssistantScreen(
    station: StationEntity?,
    tanks: List<TankEntity>,
    fuelTypes: List<FuelTypeEntity>,
    cashbox: CashboxEntity?,
    suppliers: List<SupplierEntity>,
    customers: List<CustomerEntity>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GeminiAiService(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }
    val listState = rememberLazyListState()

    var selectedModel by remember { mutableStateOf(GeminiModel.FLASH) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var isLiveVoiceMode by remember { mutableStateOf(false) }
    var liveVoiceStatus by remember { mutableStateOf("") }

    // Build rich station system instruction so the AI obeys commands with real station context
    val systemInstruction = remember(station, tanks, fuelTypes, cashbox, suppliers, customers) {
        val tankInfo = tanks.joinToString(", ") { "${it.name}: ${it.currentStockLiters}L / ${it.capacityLiters}L" }
        val priceInfo = fuelTypes.joinToString(", ") { "${it.nameArabic}: ${it.currentPrice} YER" }
        val supplierInfo = suppliers.joinToString(", ") { "${it.name} (رصيد: ${it.currentBalance} YER)" }
        val cashInfo = cashbox?.currentBalance ?: 0.0

        """
        أنت المساعد الذكي لنظام إدارة محطة الوقود (${station?.name ?: "محطة الوقود الرئيسية"}).
        دورك الأساسي هو: إطاعة وتنفيذ أوامر المستخدم، والإجابة عن الاستفسارات، ومساعدة مدير المحطة.
        
        بيانات المحطة الحالية في الوقت الفعلي:
        - الرصيد النقدي في الخزينة: $cashInfo ر.ي.
        - أسعار الوقود الحالية: $priceInfo.
        - مخزون الخزانات: $tankInfo.
        - أرصدة الموردين: $supplierInfo.
        
        تعليمات السلوك:
        1. أجب بلغة عربية سليمة وواضحة وموجزة ومباشرة.
        2. عند توجيه أمر (مثل معرفة رصيد مورد، حساب كميات، التحقق من مخزون، أسعار)، نفذ فوراً وأعط الجواب الحاسم.
        3. إذا طُلب إجراء عملية مالية، وضّح تفاصيل العملية وأرشد المستخدم للخطوة المحددة في النظام.
        4. كن مساعداً مطيعاً ومخلصاً لمدير المحطة.
        """.trimIndent()
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                role = "model",
                content = "مرحباً بك! أنا مساعدك الذكي لإدارة محطة الوقود وإطاعة كافة أوامرك. يمكنك التحدث إليّ صوتياً، أو سؤالي عن الأرصدة، والمخزون، وأسعار الوقود، وكشف حساب الموردين والعملاء.",
                modelUsed = selectedModel.displayName
            )
        )
    }

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                isRecordingAudio = true
            }
        }
    }

    fun sendMessage(textToSend: String) {
        if (textToSend.isBlank() || isLoading) return
        val userMsg = ChatMessage(role = "user", content = textToSend.trim())
        messages.add(userMsg)
        inputText = ""
        isLoading = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val result = aiService.generateChatResponse(
                messages = messages.toList(),
                systemPrompt = systemInstruction,
                selectedModel = selectedModel
            )
            isLoading = false
            val replyText = result.getOrElse { "عذراً، حدث خطأ: ${it.message}" }
            messages.add(ChatMessage(role = "model", content = replyText, modelUsed = selectedModel.displayName))
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        Card(
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = TextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مساعد المحطة الذكي (Gemini)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "مطياع لأوامر مدير المحطة ⚡",
                                style = MaterialTheme.typography.labelSmall,
                                color = Primary
                            )
                        }
                    }

                    // Live API Toggle Button
                    IconButton(
                        onClick = { isLiveVoiceMode = !isLiveVoiceMode },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isLiveVoiceMode) Color(0xFFDC2626) else BackgroundLight)
                    ) {
                        Icon(
                            Icons.Default.SpatialAudioOff,
                            contentDescription = "Live Voice API",
                            tint = if (isLiveVoiceMode) Color.White else Primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Model Selection Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        GeminiModel.FLASH,
                        GeminiModel.PRO,
                        GeminiModel.FLASH_LITE
                    ).forEach { model ->
                        val isSelected = selectedModel == model
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedModel = model },
                            label = {
                                Text(
                                    text = model.displayName.replace("Gemini ", ""),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary.copy(alpha = 0.15f),
                                selectedLabelColor = Primary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Live Mode Banner (if active)
        AnimatedVisibility(visible = isLiveVoiceMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFCA5A5))),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                "المحادثة الصوتية الحية (gemini-3.8-live)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                liveVoiceStatus.ifBlank { "اضغط مطولاً على المايك للتحدث فورياً" },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = { isLiveVoiceMode = false }) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextMuted)
                    }
                }
            }
        }

        // Quick Suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("كم رصيد الخزينة؟", "مخزون الخزانات", "أسعار اليوم", "أرصدة الموردين").forEach { prompt ->
                SuggestionChip(
                    onClick = { sendMessage(prompt) },
                    label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Chat Messages Thread
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Card(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) Primary else SurfaceWhite
                        ),
                        border = if (isUser) null else CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color.White else TextPrimary
                            )
                            if (msg.modelUsed != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.modelUsed,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = if (isUser) Color.White.copy(alpha = 0.7f) else TextMuted
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جاري معالجة الأمر...", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    }
                }
            }
        }

        // Input Bar
        Card(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Microphone / Transcribe Audio Button (gemini-3.5-transcribe / gemini-3.8-live)
                IconButton(
                    onClick = {
                        val hasMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        if (!hasMic) {
                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@IconButton
                        }

                        if (!isRecordingAudio) {
                            val file = audioRecorder.startRecording()
                            if (file != null) {
                                isRecordingAudio = true
                            }
                        } else {
                            val recordedFile = audioRecorder.stopRecording()
                            isRecordingAudio = false
                            if (recordedFile != null && recordedFile.exists()) {
                                isLoading = true
                                coroutineScope.launch {
                                    if (isLiveVoiceMode) {
                                        liveVoiceStatus = "جاري الاتصال بـ gemini-3.8-live..."
                                        val reply = aiService.liveVoiceInteraction(recordedFile, systemInstruction)
                                        liveVoiceStatus = ""
                                        isLoading = false
                                        val text = reply.getOrElse { "فشل الرد الصوتي الحي: ${it.message}" }
                                        messages.add(ChatMessage(role = "user", content = "🎤 [تسجيل صوتي حي]", isAudio = true))
                                        messages.add(ChatMessage(role = "model", content = text, modelUsed = GeminiModel.LIVE.displayName))
                                    } else {
                                        // Transcribe with gemini-3.5-transcribe
                                        val transcribedResult = aiService.transcribeAudio(recordedFile)
                                        isLoading = false
                                        val text = transcribedResult.getOrElse { "" }
                                        if (text.isNotBlank()) {
                                            inputText = text
                                        }
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingAudio) Color(0xFFDC2626) else BackgroundLight)
                ) {
                    Icon(
                        if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "تسجيل صوتي",
                        tint = if (isRecordingAudio) Color.White else Primary
                    )
                }

                // Text Input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (isRecordingAudio) "جاري التسجيل الصوتي..." else "اكتب أمراً للمساعد...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = BackgroundLight,
                        unfocusedContainerColor = BackgroundLight
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )

                // Send Button
                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) Primary else Color(0xFFE2E8F0))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال",
                        tint = if (inputText.isNotBlank()) Color.White else TextMuted
                    )
                }
            }
        }
    }
}
