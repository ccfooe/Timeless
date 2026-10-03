package com.classbell.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import com.classbell.app.llm.LlmClient

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classbell_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        const val OFFICIAL_MODEL_GLM = "glm-4v-plus"
        const val OFFICIAL_MODEL_GEMINI = "gemini-3.6-flash"

        const val DEFAULT_OFFICIAL_ENDPOINT = "https://open.bigmodel.cn/api/paas/v4"
        const val DEFAULT_OFFICIAL_MODEL = OFFICIAL_MODEL_GLM

        const val GEMINI_OFFICIAL_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/openai"
        const val GEMINI_OFFICIAL_MODEL = OFFICIAL_MODEL_GEMINI

        const val MODE_OFFICIAL = "official"
        const val MODE_CUSTOM = "custom"
        const val ROLE_TEACHER = "teacher"
        const val ROLE_STUDENT = "student"

        // 1. Obfuscated official Zhipu API key
        private val OBFUSCATED_OFFICIAL_KEY_PARTS = listOf(
            "NTY5ZWIxYWE2ZTNlNGM5",
            "MGI1NzkzMjU0MDQ0MmY4",
            "MzIuNkplUXh4MjA4SDRx",
            "alU1UQ=="
        )

        fun getOfficialApiKey(): String {
            return runCatching {
                val b64 = OBFUSCATED_OFFICIAL_KEY_PARTS.joinToString("")
                String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            }.getOrDefault("569eb1aa6e3e4c90b57932540442f832.6JeQxx208H4qjU5Q")
        }

        // 2. Deprecated / Stripped official Gemini API key for release security
        private val OBFUSCATED_GEMINI_KEY_PARTS = emptyList<String>()

        fun getOfficialGeminiApiKey(): String = ""
    }

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val AI_CHANNEL_MODE = stringPreferencesKey("ai_channel_mode")
        val OFFICIAL_ENDPOINT = stringPreferencesKey("official_endpoint")
        val OFFICIAL_MODEL = stringPreferencesKey("official_model")
        val END_WARN_MINUTES = intPreferencesKey("end_warn_minutes")
        val SURVIVAL_GUIDE_SHOWN = booleanPreferencesKey("survival_guide_shown")
        val ACTIVE_SEMESTER_ID = longPreferencesKey("active_semester_id")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val USER_ROLE = stringPreferencesKey("user_role")
        val AUTO_TIMEZONE_CONVERSION = booleanPreferencesKey("auto_timezone_conversion")
        val SYNC_SYSTEM_CALENDAR = booleanPreferencesKey("sync_system_calendar")
    }

    val syncSystemCalendarFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SYNC_SYSTEM_CALENDAR] ?: false
    }

    val userRoleFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ROLE] ?: ROLE_TEACHER
    }

    val autoTimezoneConversionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_TIMEZONE_CONVERSION] ?: false
    }

    val aiChannelModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AI_CHANNEL_MODE] ?: MODE_OFFICIAL
    }

    val officialEndpointFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[PreferencesKeys.OFFICIAL_ENDPOINT]?.trim()
        when {
            saved.isNullOrBlank() || saved.contains("workers.dev") -> DEFAULT_OFFICIAL_ENDPOINT
            !saved.startsWith("http://") && !saved.startsWith("https://") -> "https://$saved"
            else -> saved
        }
    }

    val officialModelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        val saved = preferences[PreferencesKeys.OFFICIAL_MODEL]?.trim()
        when {
            saved.isNullOrBlank() || saved == "glm-4.6v" || saved == "glm-4.5v" -> DEFAULT_OFFICIAL_MODEL
            else -> saved
        }
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.API_KEY] ?: ""
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "zh"
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "system"
    }

    val accentColorFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCENT_COLOR] ?: "blue"
    }

    val baseUrlFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.BASE_URL] ?: "https://open.bigmodel.cn/api/paas/v4"
    }

    val modelFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.MODEL] ?: "glm-4.5v"
    }

    val endWarnMinutesFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.END_WARN_MINUTES] ?: 5
    }

    val survivalGuideShownFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SURVIVAL_GUIDE_SHOWN] ?: false
    }

    val activeSemesterIdFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACTIVE_SEMESTER_ID] ?: 1L
    }

    suspend fun updateApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.API_KEY] = apiKey
        }
    }

    suspend fun updateBaseUrl(baseUrl: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_URL] = baseUrl
        }
    }

    suspend fun updateModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MODEL] = model
        }
    }

    suspend fun updateEndWarnMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.END_WARN_MINUTES] = minutes
        }
    }

    suspend fun setSurvivalGuideShown(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SURVIVAL_GUIDE_SHOWN] = shown
        }
    }

    suspend fun setActiveSemesterId(semesterId: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_SEMESTER_ID] = semesterId
        }
    }

    suspend fun updateAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = lang
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun updateAccentColor(color: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR] = color
        }
    }

    suspend fun updateAiChannelMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_CHANNEL_MODE] = mode
        }
    }

    suspend fun updateOfficialEndpoint(endpoint: String) {
        val cleanEndpoint = when {
            endpoint.isBlank() || endpoint.contains("workers.dev") -> DEFAULT_OFFICIAL_ENDPOINT
            !endpoint.startsWith("http://") && !endpoint.startsWith("https://") -> "https://${endpoint.trim()}"
            else -> endpoint.trim()
        }
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFICIAL_ENDPOINT] = cleanEndpoint
        }
    }

    suspend fun updateOfficialModel(model: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OFFICIAL_MODEL] = model
        }
    }

    suspend fun updateUserRole(role: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ROLE] = role
        }
    }

    suspend fun updateAutoTimezoneConversion(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_TIMEZONE_CONVERSION] = enabled
        }
    }

    suspend fun updateSyncSystemCalendar(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYNC_SYSTEM_CALENDAR] = enabled
        }
    }

    suspend fun resolveLlmConfig(): Result<ResolvedLlmConfig> {
        val mode = aiChannelModeFlow.firstOrNull() ?: MODE_OFFICIAL
        return if (mode == MODE_OFFICIAL) {
            val endpoint = officialEndpointFlow.firstOrNull()?.let { ep ->
                if (ep.isBlank() || ep.contains("generativelanguage") || ep.contains("workers.dev")) {
                    DEFAULT_OFFICIAL_ENDPOINT
                } else ep
            } ?: DEFAULT_OFFICIAL_ENDPOINT
            val key = getOfficialApiKey()
            Result.success(ResolvedLlmConfig(endpoint, key, DEFAULT_OFFICIAL_MODEL))
        } else {
            val apiKey = apiKeyFlow.firstOrNull() ?: ""
            val baseUrl = baseUrlFlow.firstOrNull() ?: ""
            val model = modelFlow.firstOrNull() ?: ""
            if (apiKey.isBlank()) {
                Result.failure(Exception("请先在「设置 - AI 识别设置」中配置 API Key"))
            } else {
                Result.success(ResolvedLlmConfig(baseUrl, apiKey, model))
            }
        }
    }

    suspend fun createLlmClient(): Result<LlmClient> {
        return resolveLlmConfig().map { config ->
            LlmClient(config.baseUrl, config.apiKey, config.model)
        }
    }
}

data class ResolvedLlmConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
