package com.classbell.app.llm

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = normalizeEndpoint(baseUrl)
            val requestBody = OpenAiChatRequest(
                model = model,
                messages = listOf(
                    OpenAiMessage(
                        role = "user",
                        content = listOf(
                            OpenAiContentPart(type = "text", text = "Ping. Reply with 'OK'.")
                        )
                    )
                ),
                temperature = 0.0
            )

            val bodyJson = json.encodeToString(requestBody)
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errMsg = when (code) {
                        401 -> "API Key 无效 (401)"
                        429 -> "额度不足或被限流 (429)"
                        else -> "连接失败: HTTP $code"
                    }
                    throw Exception(errMsg)
                }
                "连接成功！"
            }
        }
    }

    suspend fun extractTimetable(imageStream: InputStream): Result<List<ParsedCourseItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base64Image = downscaleAndEncodeImage(imageStream)
                val rawResponse = callMultimodalApi(
                    prompt = LlmPrompts.TIMETABLE_EXTRACTION_PROMPT,
                    base64DataUri = "data:image/jpeg;base64,$base64Image"
                )
                val cleanJson = extractJsonFromMarkdown(rawResponse)
                json.decodeFromString<List<ParsedCourseItem>>(cleanJson)
            }
        }

    suspend fun extractCalendar(imageStream: InputStream): Result<ParsedCalendarResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base64Image = downscaleAndEncodeImage(imageStream)
                val rawResponse = callMultimodalApi(
                    prompt = LlmPrompts.CALENDAR_EXTRACTION_PROMPT,
                    base64DataUri = "data:image/jpeg;base64,$base64Image"
                )
                val cleanJson = extractJsonFromMarkdown(rawResponse)
                json.decodeFromString<ParsedCalendarResult>(cleanJson)
            }
        }

    private fun callMultimodalApi(prompt: String, base64DataUri: String): String {
        val url = normalizeEndpoint(baseUrl)
        val requestObj = OpenAiChatRequest(
            model = model,
            messages = listOf(
                OpenAiMessage(
                    role = "user",
                    content = listOf(
                        OpenAiContentPart(type = "text", text = prompt),
                        OpenAiContentPart(
                            type = "image_url",
                            imageUrl = OpenAiImageUrl(url = base64DataUri)
                        )
                    )
                )
            ),
            temperature = 0.0
        )

        val requestBodyJson = json.encodeToString(requestObj)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBodyJson.toRequestBody("application/json".toMediaType()))
            .build()

        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string() ?: ""
                val msg = when (code) {
                    401 -> "API Key 无效 (401)"
                    429 -> "额度不足或被限流 (429)"
                    504 -> "请求超时 (504)"
                    else -> "API 请求失败 ($code): $errBody"
                }
                throw Exception(msg)
            }

            val respBody = response.body?.string() ?: throw Exception("响应体为空")
            val chatResponse = json.decodeFromString<OpenAiChatResponse>(respBody)
            chatResponse.choices.firstOrNull()?.message?.content
                ?: throw Exception("模型未返回有效文本内容")
        }
    }

    private fun downscaleAndEncodeImage(inputStream: InputStream): String {
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("无法读取图片数据")

        val maxEdge = 2048
        val width = originalBitmap.width
        val height = originalBitmap.height

        val scaledBitmap = if (width > maxEdge || height > maxEdge) {
            val scale = if (width > height) maxEdge.toFloat() / width else maxEdge.toFloat() / height
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun extractJsonFromMarkdown(text: String): String {
        val trimmed = text.trim()
        val jsonPattern = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
        val match = jsonPattern.find(trimmed)
        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            trimmed
        }
    }

    private fun normalizeEndpoint(url: String): String {
        var u = url.trimEnd('/')
        if (!u.endsWith("/chat/completions")) {
            u = "$u/chat/completions"
        }
        return u
    }
}
