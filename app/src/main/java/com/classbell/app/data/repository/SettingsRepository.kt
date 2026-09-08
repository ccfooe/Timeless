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
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "classbell_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val API_KEY = stringPreferencesKey("api_key")
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val END_WARN_MINUTES = intPreferencesKey("end_warn_minutes")
        val SURVIVAL_GUIDE_SHOWN = booleanPreferencesKey("survival_guide_shown")
        val ACTIVE_SEMESTER_ID = longPreferencesKey("active_semester_id")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val apiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.API_KEY] ?: ""
    }

    val appLanguageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "zh"
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
}
