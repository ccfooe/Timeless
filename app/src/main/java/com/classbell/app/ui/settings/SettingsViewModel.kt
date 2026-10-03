package com.classbell.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.core.CalendarSyncManager
import com.classbell.app.data.backup.BackupManager
import com.classbell.app.data.backup.ImportMode
import com.classbell.app.data.local.ClassBellDatabase
import com.classbell.app.data.repository.AffairRepository
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.data.repository.ScheduleRepository
import com.classbell.app.data.repository.SemesterRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.llm.LlmClient
import com.classbell.app.service.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class SettingsUiState(
    val aiChannelMode: String = SettingsRepository.MODE_OFFICIAL,
    val officialEndpoint: String = SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT,
    val officialModel: String = SettingsRepository.DEFAULT_OFFICIAL_MODEL,
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val endWarnMinutes: Int = 5,
    val appLanguage: String = "zh",
    val themeMode: String = "system",
    val accentColor: String = "blue",
    val userRole: String = SettingsRepository.ROLE_TEACHER,
    val autoTimezoneConversion: Boolean = false,
    val syncSystemCalendar: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val showLlmDialog: Boolean = false,
    val showSurvivalDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showThemeModeDialog: Boolean = false,
    val showAccentColorDialog: Boolean = false,
    val showLockscreenDialog: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val backupJsonString: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val semesterRepository: SemesterRepository,
    private val periodRepository: PeriodRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Collect repository preferences safely in viewModelScope
        viewModelScope.launch {
            settingsRepository.userRoleFlow.catch { }.collectLatest { role ->
                _uiState.update { it.copy(userRole = role) }
            }
        }
        viewModelScope.launch {
            settingsRepository.aiChannelModeFlow.catch { }.collectLatest { mode ->
                _uiState.update { it.copy(aiChannelMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.officialEndpointFlow.catch { }.collectLatest { ep ->
                _uiState.update { it.copy(officialEndpoint = ep) }
            }
        }
        viewModelScope.launch {
            settingsRepository.officialModelFlow.catch { }.collectLatest { mdl ->
                _uiState.update { it.copy(officialModel = mdl) }
            }
        }
        viewModelScope.launch {
            settingsRepository.apiKeyFlow.catch { }.collectLatest { key ->
                _uiState.update { it.copy(apiKey = key) }
            }
        }
        viewModelScope.launch {
            settingsRepository.baseUrlFlow.catch { }.collectLatest { url ->
                _uiState.update { it.copy(baseUrl = url) }
            }
        }
        viewModelScope.launch {
            settingsRepository.modelFlow.catch { }.collectLatest { mdl ->
                _uiState.update { it.copy(model = mdl) }
            }
        }
        viewModelScope.launch {
            settingsRepository.endWarnMinutesFlow.catch { }.collectLatest { warn ->
                _uiState.update { it.copy(endWarnMinutes = warn) }
            }
        }
        viewModelScope.launch {
            settingsRepository.appLanguageFlow.catch { }.collectLatest { lang ->
                _uiState.update { it.copy(appLanguage = lang) }
            }
        }
        viewModelScope.launch {
            settingsRepository.themeModeFlow.catch { }.collectLatest { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.accentColorFlow.catch { }.collectLatest { color ->
                _uiState.update { it.copy(accentColor = color) }
            }
        }
        viewModelScope.launch {
            settingsRepository.autoTimezoneConversionFlow.catch { }.collectLatest { autoTz ->
                _uiState.update { it.copy(autoTimezoneConversion = autoTz) }
            }
        }
        viewModelScope.launch {
            settingsRepository.syncSystemCalendarFlow.catch { }.collectLatest { sync ->
                _uiState.update { it.copy(syncSystemCalendar = sync) }
            }
        }
    }

    fun setAutoTimezoneConversion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoTimezoneConversion(enabled)
            _uiState.update { it.copy(autoTimezoneConversion = enabled) }
        }
    }

    fun setSyncSystemCalendar(
        enabled: Boolean,
        context: Context,
        database: ClassBellDatabase,
        onPermissionNeeded: () -> Unit
    ) {
        if (enabled && !CalendarSyncManager.hasCalendarPermissions(context)) {
            onPermissionNeeded()
            return
        }
        viewModelScope.launch {
            settingsRepository.updateSyncSystemCalendar(enabled)
            _uiState.update { it.copy(syncSystemCalendar = enabled) }
            if (enabled) {
                CalendarSyncManager.syncAll(context, database)
            } else {
                CalendarSyncManager.clearCalendar(context)
            }
        }
    }

    fun openThemeModeDialog() {
        _uiState.update { it.copy(showThemeModeDialog = true) }
    }

    fun closeThemeModeDialog() {
        _uiState.update { it.copy(showThemeModeDialog = false) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
            _uiState.update { it.copy(showThemeModeDialog = false, themeMode = mode) }
        }
    }

    fun openAccentColorDialog() {
        _uiState.update { it.copy(showAccentColorDialog = true) }
    }

    fun closeAccentColorDialog() {
        _uiState.update { it.copy(showAccentColorDialog = false) }
    }

    fun setAccentColor(color: String) {
        viewModelScope.launch {
            settingsRepository.updateAccentColor(color)
            _uiState.update { it.copy(showAccentColorDialog = false, accentColor = color) }
        }
    }

    fun openLockscreenDialog() {
        _uiState.update { it.copy(showLockscreenDialog = true) }
    }

    fun closeLockscreenDialog() {
        _uiState.update { it.copy(showLockscreenDialog = false) }
    }

    fun openLlmDialog() {
        _uiState.update { it.copy(showLlmDialog = true, testConnectionMessage = null) }
    }

    fun closeLlmDialog() {
        _uiState.update { it.copy(showLlmDialog = false) }
    }

    fun openLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = true) }
    }

    fun closeLanguageDialog() {
        _uiState.update { it.copy(showLanguageDialog = false) }
    }

    fun openSurvivalGuide() {
        _uiState.update { it.copy(showSurvivalDialog = true) }
    }

    fun closeSurvivalGuide() {
        _uiState.update { it.copy(showSurvivalDialog = false) }
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(lang)
            _uiState.update { it.copy(showLanguageDialog = false, appLanguage = lang) }
        }
    }

    fun saveLlmSettings(
        aiChannelMode: String,
        officialEndpoint: String,
        officialModel: String,
        apiKey: String,
        baseUrl: String,
        model: String
    ) {
        viewModelScope.launch {
            settingsRepository.updateAiChannelMode(aiChannelMode)
            settingsRepository.updateOfficialEndpoint(officialEndpoint.trim())
            settingsRepository.updateOfficialModel(officialModel.trim())
            settingsRepository.updateApiKey(apiKey.trim())
            settingsRepository.updateBaseUrl(baseUrl.trim())
            settingsRepository.updateModel(model.trim())
            _uiState.update {
                it.copy(
                    aiChannelMode = aiChannelMode,
                    officialEndpoint = officialEndpoint.trim(),
                    officialModel = officialModel.trim(),
                    apiKey = apiKey.trim(),
                    baseUrl = baseUrl.trim(),
                    model = model.trim(),
                    showLlmDialog = false
                )
            }
        }
    }

    fun testLlmConnection(
        mode: String,
        officialEndpoint: String,
        officialModel: String,
        apiKey: String,
        baseUrl: String,
        model: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true, testConnectionMessage = "正在测试连接...") }
            val (targetUrl, targetKey, targetModel) = if (mode == SettingsRepository.MODE_OFFICIAL) {
                val ep = officialEndpoint.trim().let {
                    if (it.isBlank() || it.contains("generativelanguage") || it.contains("workers.dev")) {
                        SettingsRepository.DEFAULT_OFFICIAL_ENDPOINT
                    } else it
                }
                Triple(ep, SettingsRepository.getOfficialApiKey(), SettingsRepository.DEFAULT_OFFICIAL_MODEL)
            } else {
                Triple(baseUrl.trim(), apiKey.trim(), model.trim())
            }
            val client = LlmClient(targetUrl, targetKey, targetModel)
            val result = client.testConnection()
            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    testConnectionMessage = result.fold(
                        onSuccess = { msg -> "✓ $msg" },
                        onFailure = { err -> err.message ?: "连接失败" }
                    )
                )
            }
        }
    }

    fun updateUserRole(role: String) {
        viewModelScope.launch {
            settingsRepository.updateUserRole(role)
            _uiState.update { it.copy(userRole = role) }
        }
    }

    fun updateWarnMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateEndWarnMinutes(minutes)
            _uiState.update { it.copy(endWarnMinutes = minutes) }
        }
    }

    fun openClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = true) }
    }

    fun closeClearConfirmDialog() {
        _uiState.update { it.copy(showClearConfirmDialog = false) }
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            backupManager.clearAllData()
            _uiState.update { it.copy(showClearConfirmDialog = false) }
            onDone()
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupManager.exportToJson()
            onResult(json)
        }
    }

    fun importBackup(
        jsonString: String,
        context: Context,
        database: ClassBellDatabase,
        alarmScheduler: AlarmScheduler,
        scheduleRepository: ScheduleRepository,
        affairRepository: AffairRepository,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = backupManager.importFromJson(jsonString, ImportMode.REPLACE)
            if (result.isSuccess) {
                withContext(Dispatchers.IO) {
                    val today = LocalDate.now()
                    val meetings = scheduleRepository.getMeetingsOnDate(today).firstOrNull() ?: emptyList()
                    val warnMin = settingsRepository.endWarnMinutesFlow.firstOrNull() ?: 5
                    for (m in meetings) {
                        alarmScheduler.scheduleClassAlarms(m, warnMin)
                    }
                    val affairs = affairRepository.getAllAffairsSync()
                    for (a in affairs) {
                        alarmScheduler.scheduleAffairAlarm(a)
                    }
                    CalendarSyncManager.syncIfEnabled(context, database, settingsRepository)
                }
            }
            onComplete(result.isSuccess)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val semesterRepository: SemesterRepository,
        private val periodRepository: PeriodRepository,
        private val backupManager: BackupManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(
                settingsRepository,
                semesterRepository,
                periodRepository,
                backupManager
            ) as T
        }
    }
}
