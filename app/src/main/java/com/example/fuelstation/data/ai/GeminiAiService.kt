package com.example.fuelstation.data.ai

import android.content.Context
import android.media.MediaRecorder
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.TimeUnit

enum class GeminiModel(val modelId: String, val displayName: String, val description: String) {
    FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "للمهام العامة والتشغيل اليومي"),
    PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "للتحليلات والمهام الحسابية المعقدة"),
    FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite", "للاستجابة السريعة والفورية"),
    TRANSCRIBE("gemini-3.5-transcribe", "Gemini 3.5 Transcribe", "تحويل الصوت إلى نص دقيق"),
    LIVE("gemini-3.8-live", "Gemini 3.8 Live", "محادثة صوتية حية فورية")
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user", "model", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isAudio: Boolean = false,
    val modelUsed: String? = null
)

class GeminiAiService(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY.ifBlank { "" }

    /**
     * Multi-turn chat generation with system instruction and history
     */
    suspend fun generateChatResponse(
        messages: List<ChatMessage>,
        systemPrompt: String,
        selectedModel: GeminiModel = GeminiModel.FLASH
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey
            if (key.isBlank()) {
                return@runCatching "مفتاح API غير متوفر. يرجى ضبط GEMINI_API_KEY في إعدادات البيئة لتفعيل المساعد الذكي."
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/${selectedModel.modelId}:generateContent?key=$key"

            val contentsArray = JSONArray()
            messages.filter { it.role != "system" }.takeLast(15).forEach { msg ->
                val partObj = JSONObject().put("text", msg.content)
                val contentObj = JSONObject()
                    .put("role", if (msg.role == "user") "user" else "model")
                    .put("parts", JSONArray().put(partObj))
                contentsArray.put(contentObj)
            }

            val payload = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 1024)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                // Try fallback to gemini-3.5-flash if preview model is unavailable
                if (selectedModel != GeminiModel.FLASH) {
                    return@runCatching generateChatResponse(messages, systemPrompt, GeminiModel.FLASH).getOrThrow()
                }
                throw RuntimeException("خطأ في الاتصال بالذكاء الاصطناعي: ${response.code} $respBody")
            }

            val json = JSONObject(respBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                if (parts != null && parts.length() > 0) {
                    return@runCatching parts.getJSONObject(0).optString("text", "لم يصل رد من المساعد.")
                }
            }
            "لم يتم استلام نص في الرد."
        }
    }

    /**
     * Transcribe Audio using model gemini-3.5-transcribe
     */
    suspend fun transcribeAudio(
        audioFile: File,
        mimeType: String = "audio/mp4"
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey
            if (key.isBlank()) {
                return@runCatching "يرجى تعيين مفتاح GEMINI_API_KEY لاستخدام التفريغ الصوتي."
            }

            val bytes = FileInputStream(audioFile).use { it.readBytes() }
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            // Try with gemini-3.5-transcribe, fallback to gemini-3.5-flash
            val modelsToTry = listOf(GeminiModel.TRANSCRIBE.modelId, GeminiModel.FLASH.modelId)
            var lastError: Exception? = null

            for (model in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
                    val inlineData = JSONObject()
                        .put("mimeType", mimeType)
                        .put("data", base64Data)

                    val promptPart = JSONObject().put("text", "استمع إلى هذا التسجيل الصوتي باللغة العربية وفرغه بدقة تامة كلمة بكلمة دون أي تعليقات إضافية.")

                    val payload = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(promptPart).put(JSONObject().put("inlineData", inlineData)))))
                    }

                    val request = Request.Builder()
                        .url(url)
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val json = JSONObject(respBody)
                        val text = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        if (text.isNotBlank()) return@runCatching text.trim()
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: RuntimeException("فشل تفريغ الصوت.")
        }
    }

    /**
     * Live Voice Interaction using model gemini-3.8-live
     */
    suspend fun liveVoiceInteraction(
        audioFile: File,
        systemContext: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = apiKey
            if (key.isBlank()) {
                return@runCatching "يرجى تعيين مفتاح GEMINI_API_KEY لتفعيل المحادثات الصوتية الحية."
            }

            val bytes = FileInputStream(audioFile).use { it.readBytes() }
            val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val modelsToTry = listOf(GeminiModel.LIVE.modelId, GeminiModel.FLASH.modelId)
            var lastError: Exception? = null

            for (model in modelsToTry) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
                    val inlineData = JSONObject()
                        .put("mimeType", "audio/mp4")
                        .put("data", base64Data)

                    val promptPart = JSONObject().put("text", "استمع إلى أمر المستخدم الصوتي باللغة العربية وقدم رداً صوتياً فورياً وموجزاً ينفذ الأمر الموجه لإدارة محطة الوقود.")

                    val payload = JSONObject().apply {
                        put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(promptPart).put(JSONObject().put("inlineData", inlineData)))))
                        put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemContext))))
                    }

                    val request = Request.Builder()
                        .url(url)
                        .post(payload.toString().toRequestBody("application/json".toMediaType()))
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    val respBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val json = JSONObject(respBody)
                        val text = json.optJSONArray("candidates")
                            ?.optJSONObject(0)
                            ?.optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.optJSONObject(0)
                            ?.optString("text", "") ?: ""
                        if (text.isNotBlank()) return@runCatching text.trim()
                    }
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: RuntimeException("فشلت المحادثة الصوتية الحية.")
        }
    }
}
