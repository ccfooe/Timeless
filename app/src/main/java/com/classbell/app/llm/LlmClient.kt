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
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class LlmClient(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val url = normalizeEndpoint(baseUrl)
        runCatching {
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
                        401 -> "API Key 无效或未授权 (401)"
                        403 -> "访问被拒绝 (403)，可能是境外/地区 IP 受到服务商限制或风控"
                        404 -> "云端服务接口未找到 (404)，请检查 Worker 部署"
                        429 -> "额度不足或被限流 (429)，请稍后再试"
                        502, 503 -> "云端服务暂时不可用 ($code)，请稍后重试"
                        504 -> "请求超时 (504)，请检查网络"
                        else -> "连接失败: HTTP $code"
                    }
                    throw Exception(errMsg)
                }
                val displayModel = when {
                    model.contains("glm", ignoreCase = true) -> "Timeless 官方智能云"
                    model.contains("gemini", ignoreCase = true) -> "Google Gemini"
                    model.isNotBlank() -> model
                    else -> "Timeless 智能云"
                }
                "连接成功！$displayModel 节点服务正常"
            }
        }.recoverCatching { e ->
            throw Exception(extractFriendlyErrorMessage(e, url))
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
                if (cleanJson.startsWith("[")) {
                    json.decodeFromString<List<ParsedCourseItem>>(cleanJson)
                } else if (cleanJson.startsWith("{")) {
                    // If model wrapped array inside an object e.g. { "courses": [...] }
                    val startBracket = cleanJson.indexOf('[')
                    val endBracket = cleanJson.lastIndexOf(']')
                    if (startBracket != -1 && endBracket != -1 && endBracket > startBracket) {
                        val arrayJson = cleanJson.substring(startBracket, endBracket + 1)
                        json.decodeFromString<List<ParsedCourseItem>>(arrayJson)
                    } else {
                        json.decodeFromString<List<ParsedCourseItem>>(cleanJson)
                    }
                } else {
                    json.decodeFromString<List<ParsedCourseItem>>(cleanJson)
                }
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
                if (cleanJson.startsWith("{")) {
                    json.decodeFromString<ParsedCalendarResult>(cleanJson)
                } else if (cleanJson.startsWith("[")) {
                    // Model returned a holiday array directly
                    val holidays = json.decodeFromString<List<ParsedHolidayItem>>(cleanJson)
                    ParsedCalendarResult(
                        semesterName = "校历与假期安排",
                        week1Monday = LocalDate.now().toString(),
                        teachingWeeks = ParsedWeekRange(1, 18),
                        holidays = holidays
                    )
                } else {
                    json.decodeFromString<ParsedCalendarResult>(cleanJson)
                }
            }
        }

    suspend fun extractPeriodTable(imageStream: InputStream): Result<List<ParsedPeriodSlotItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base64Image = downscaleAndEncodeImage(imageStream)
                val rawResponse = callMultimodalApi(
                    prompt = LlmPrompts.PERIOD_TABLE_EXTRACTION_PROMPT,
                    base64DataUri = "data:image/jpeg;base64,$base64Image"
                )
                val cleanJson = extractJsonFromMarkdown(rawResponse)
                json.decodeFromString<List<ParsedPeriodSlotItem>>(cleanJson)
            }
        }

    suspend fun extractAffair(
        rawText: String,
        refDate: LocalDate,
        userZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Result<ParsedAffair> =
        withContext(Dispatchers.IO) {
            runCatching {
                val prompt = LlmPrompts.getAffairExtractionPrompt(refDate, userZone)
                val rawResponse = callTextApi(
                    prompt = prompt,
                    userText = rawText
                )
                val cleanJson = extractJsonFromMarkdown(rawResponse)
                json.decodeFromString<ParsedAffair>(cleanJson)
            }
        }

    suspend fun extractUniversal(
        rawText: String?,
        imageStream: InputStream?,
        refDate: LocalDate,
        userZone: java.time.ZoneId = java.time.ZoneId.systemDefault()
    ): Result<UniversalIngestResult> = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = LlmPrompts.getUniversalIngestPrompt(refDate, userZone)
            val rawResponse = if (imageStream != null) {
                val base64Image = downscaleAndEncodeImage(imageStream)
                val fullPrompt = if (!rawText.isNullOrBlank()) {
                    "$prompt\n\n【用户补充提供的文字说明/通知如下】：\n$rawText"
                } else prompt
                callMultimodalApi(
                    prompt = fullPrompt,
                    base64DataUri = "data:image/jpeg;base64,$base64Image"
                )
            } else if (!rawText.isNullOrBlank()) {
                callTextApi(
                    prompt = prompt,
                    userText = rawText
                )
            } else {
                throw Exception("请提供文字通知内容或上传图片截图")
            }

            val cleanJson = extractJsonFromMarkdown(rawResponse)
            json.decodeFromString<UniversalIngestResult>(cleanJson)
        }
    }

    private fun callTextApi(prompt: String, userText: String): String {
        val url = normalizeEndpoint(baseUrl)
        val isGoogleApi = url.contains("generativelanguage") || url.contains("googleapis.com")
        var currentModel = if (isGoogleApi && model.equals("gemini-3.8-flash", ignoreCase = true)) {
            "gemini-3.6-flash"
        } else model

        val combinedText = "$prompt\n\n【待解析的通知/便签文本如下】：\n$userText"
        val maxAttempts = if (isGoogleApi) 3 else 1
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            val requestObj = OpenAiChatRequest(
                model = currentModel,
                messages = listOf(
                    OpenAiMessage(
                        role = "user",
                        content = listOf(
                            OpenAiContentPart(type = "text", text = combinedText)
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

            try {
                val (isSuccess, code, body) = client.newCall(request).execute().use { response ->
                    Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
                }

                if (isSuccess) {
                    val chatResponse = json.decodeFromString<OpenAiChatResponse>(body)
                    return chatResponse.choices.firstOrNull()?.message?.content
                        ?: throw Exception("模型未返回有效文本内容")
                }

                if (isGoogleApi && (code in 502..503 || code == 429) && attempt < maxAttempts) {
                    currentModel = when (currentModel) {
                        "gemini-3.8-flash" -> "gemini-3.6-flash"
                        "gemini-3.6-flash" -> "gemini-3.5-flash"
                        else -> "gemini-3.6-flash"
                    }
                    try {
                        Thread.sleep(1200)
                    } catch (_: InterruptedException) {}
                    continue
                }

                val msg = when {
                    code == 401 -> "API Key 无效或未授权 (401)"
                    code == 403 -> "访问被拒绝 (403)，可能是境外/地区 IP 受到服务商限制或风控"
                    code == 400 && (body.contains("1301") || body.contains("contentFilter") || body.contains("敏感内容")) ->
                        "AI 内容安全审核拦截 (1301)：文本内容触发了国内大模型的合规关键词过滤规则。建议：1. 适当简化或去除敏感词后重试；2. 在「设置 -> AI 通道」切换为 OpenAI (GPT-4o) 等国际模型。"
                    code == 404 -> "云端服务接口未找到 (404)，请检查 Worker 部署"
                    code == 429 -> if (isGoogleApi) "Google 官方接口调用频率超限 (429)，请稍等片刻后再试或换用智谱专线" else "云端服务额度超限或限流 (429)，请稍后重试或在设置中切换自定义 API"
                    code in 502..503 -> if (isGoogleApi) "Google 官方服务节点高峰期暂时过载 (503)，请稍候重试或换用智谱专线" else "云端服务暂时不可用 ($code)，请稍后重试"
                    code == 504 -> "请求超时 (504)，请检查网络连接"
                    else -> "API 请求失败 ($code): $body"
                }
                throw Exception(msg)
            } catch (e: Exception) {
                lastException = e
                if (isGoogleApi && attempt < maxAttempts && (e is java.net.SocketTimeoutException || e.message?.contains("503") == true)) {
                    currentModel = "gemini-3.6-flash"
                    try {
                        Thread.sleep(1200)
                    } catch (_: InterruptedException) {}
                    continue
                }
                throw Exception(extractFriendlyErrorMessage(e, url))
            }
        }
        throw Exception(extractFriendlyErrorMessage(lastException ?: Exception("请求失败"), url))
    }

    private fun callMultimodalApi(prompt: String, base64DataUri: String): String {
        val url = normalizeEndpoint(baseUrl)
        val isGoogleApi = url.contains("generativelanguage") || url.contains("googleapis.com")
        var currentModel = if (isGoogleApi && model.equals("gemini-3.8-flash", ignoreCase = true)) {
            "gemini-3.6-flash"
        } else model

        val maxAttempts = if (isGoogleApi) 3 else 1
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            val requestObj = OpenAiChatRequest(
                model = currentModel,
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

            try {
                val (isSuccess, code, body) = client.newCall(request).execute().use { response ->
                    Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
                }

                if (isSuccess) {
                    val chatResponse = json.decodeFromString<OpenAiChatResponse>(body)
                    return chatResponse.choices.firstOrNull()?.message?.content
                        ?: throw Exception("模型未返回有效文本内容")
                }

                // If Google 503 (Overloaded) or 429 (Rate limit), retry with fallback model
                if (isGoogleApi && (code in 502..503 || code == 429) && attempt < maxAttempts) {
                    currentModel = when (currentModel) {
                        "gemini-3.8-flash" -> "gemini-3.6-flash"
                        "gemini-3.6-flash" -> "gemini-3.5-flash"
                        else -> "gemini-3.6-flash"
                    }
                    try {
                        Thread.sleep(1200)
                    } catch (_: InterruptedException) {}
                    continue
                }

                val msg = when {
                    code == 401 -> "API Key 无效或未授权 (401)"
                    code == 403 -> "访问被拒绝 (403)，可能是境外/地区 IP 受到服务商限制或风控"
                    code == 400 && (body.contains("1301") || body.contains("contentFilter") || body.contains("敏感内容")) ->
                        "AI 内容安全审核拦截 (1301)：校历或课表截图中的标题、校徽或特定节日触发了国内大模型（智谱）的合规安全审查。\n\n解决办法：\n1. 裁剪图片：用手机相册裁剪掉顶部的“中华民国”标题栏及校徽后重新识别；\n2. 切换模型：在「设置 -> AI 模型通道」切换为不受国内审查限制的国际模型（如 GPT-4o、Google Gemini）。"
                    code == 404 -> "云端服务接口未找到 (404)，请检查 Worker 部署"
                    code == 429 -> if (isGoogleApi) "Google 官方接口调用频率超限 (429)，请稍等片刻后再试或换用智谱专线" else "云端服务额度超限或限流 (429)，请稍后重试或在设置中切换自定义 API"
                    code in 502..503 -> if (isGoogleApi) "Google 官方服务节点高峰期暂时过载 (503)，请稍候重试或换用智谱专线" else "云端服务暂时不可用 ($code)，请稍后重试"
                    code == 504 -> "请求超时 (504)，请检查网络连接"
                    else -> "API 请求失败 ($code): $body"
                }
                throw Exception(msg)
            } catch (e: Exception) {
                lastException = e
                if (isGoogleApi && attempt < maxAttempts && (e is java.net.SocketTimeoutException || e.message?.contains("503") == true)) {
                    currentModel = "gemini-3.6-flash"
                    try {
                        Thread.sleep(1200)
                    } catch (_: InterruptedException) {}
                    continue
                }
                throw Exception(extractFriendlyErrorMessage(e, url))
            }
        }
        throw Exception(extractFriendlyErrorMessage(lastException ?: Exception("请求失败"), url))
    }

    private fun extractFriendlyErrorMessage(e: Throwable, targetUrl: String): String {
        val isWorkersDev = targetUrl.contains("workers.dev")
        val isGoogleApi = targetUrl.contains("googleapis.com") || targetUrl.contains("generativelanguage")
        val msg = e.message ?: ""
        return when {
            e is java.net.SocketTimeoutException || msg.contains("ETIMEDOUT", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) -> {
                if (isWorkersDev) {
                    "连接超时 (ETIMEDOUT)：国内手机网络无法直连 Cloudflare 默认域名 (*.workers.dev)。\n建议：在 Cloudflare 免费绑定自定义域名（如 api.yourdomain.com 即可国内秒连），或开启手机网络代理。"
                } else if (isGoogleApi) {
                    "连接超时：当前网络无法直接访问 Google 官方服务节点。\n\n💡 原因与建议：\n1. Google 服务在中国大陆移动蜂窝网络下受限，若在国内直连网络使用，请在设置中切换为【智谱 GLM-4v-Plus】（国内秒级直连，开箱即用）；\n2. 若需使用 Gemini 模型，请开启手机网络代理（VPN），或由港澳台及海外网络直接访问（免密即用）。"
                } else {
                    "连接超时，请检查网络连接或节点地址"
                }
            }
            e is java.net.ConnectException || e is javax.net.ssl.SSLException -> {
                if (isWorkersDev) {
                    "无法连接服务器：国内手机网络对 *.workers.dev 存在网络阻断。建议在 Cloudflare 绑定自定义域名即可直连。"
                } else if (isGoogleApi) {
                    "无法连接 Google 服务：国内网络存在访问阻断。\n建议在设置中切换为【智谱 GLM-4v-Plus】国内直连通道，或开启网络代理。"
                } else {
                    "无法连接到服务器，请检查网络连接或网址"
                }
            }
            e is java.net.UnknownHostException -> {
                if (isGoogleApi) {
                    "无法解析 Google 域名：国内网络无法直接寻址。\n建议在设置中切换为【智谱 GLM-4v-Plus】国内直连通道，或开启网络代理。"
                } else {
                    "无法解析域名，请检查网址是否完整正确"
                }
            }
            else -> e.message ?: "网络请求异常"
        }
    }

    private fun downscaleAndEncodeImage(inputStream: InputStream): String {
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw Exception("无法读取图片数据")

        val width = originalBitmap.width
        val height = originalBitmap.height
        val aspectRatio = if (width > 0) height.toFloat() / width else 1f

        // Intelligent downscaling:
        // For mobile screenshots (tall aspect ratio > 2.0), keep width legible (up to 1080px)
        // and cap maximum height at 2500px, avoiding small font blurriness in long notices.
        val (targetWidth, targetHeight) = when {
            aspectRatio > 2.0f -> {
                val clampedW = minOf(width, 1080)
                val clampedH = (clampedW * aspectRatio).toInt().coerceAtMost(2500)
                Pair(clampedW, clampedH)
            }
            maxOf(width, height) > 1600 -> {
                val scale = 1600f / maxOf(width, height)
                Pair((width * scale).toInt(), (height * scale).toInt())
            }
            else -> Pair(width, height)
        }

        val scaledBitmap = if (targetWidth != width || targetHeight != height) {
            Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        // 75% quality yields crisp OCR text while keeping payload ~200-350KB (< 0.5s network upload)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun extractJsonFromMarkdown(text: String): String {
        val trimmed = text.trim()
        val jsonPattern = Regex("""```(?:json)?\s*([\s\S]*?)\s*```""", RegexOption.IGNORE_CASE)
        val match = jsonPattern.find(trimmed)
        if (match != null) {
            return match.groupValues[1].trim()
        }

        val startBrace = trimmed.indexOf('{')
        val startBracket = trimmed.indexOf('[')

        // If '{' comes before '[' or there is no '[', outer container is a JSON object!
        if (startBrace != -1 && (startBracket == -1 || startBrace < startBracket)) {
            val endBrace = trimmed.lastIndexOf('}')
            if (endBrace > startBrace) {
                return trimmed.substring(startBrace, endBrace + 1).trim()
            }
        }

        // If '[' comes before '{' or there is no '{', outer container is a JSON array!
        if (startBracket != -1 && (startBrace == -1 || startBracket < startBrace)) {
            val endBracket = trimmed.lastIndexOf(']')
            if (endBracket > startBracket) {
                return trimmed.substring(startBracket, endBracket + 1).trim()
            }
        }

        return trimmed
    }

    private fun normalizeEndpoint(url: String): String {
        var u = url.trim().trimEnd('/')
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://$u"
        }
        if (!u.endsWith("/chat/completions")) {
            u = "$u/chat/completions"
        }
        return u
    }
}
