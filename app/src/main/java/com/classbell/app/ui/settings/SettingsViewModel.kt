package com.classbell.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classbell.app.data.backup.BackupManager
import com.classbell.app.data.backup.ImportMode
import com.classbell.app.data.repository.PeriodRepository
import com.classbell.app.data.repository.SemesterRepository
import com.classbell.app.data.repository.SettingsRepository
import com.classbell.app.llm.LlmClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = "",
    val model: String = "",
    val endWarnMinutes: Int = 5,
    val appLanguage: String = "zh",
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val showLlmDialog: Boolean = false,
    val showSurvivalDialog: Boolean = false,
    val showLanguageDialog: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val backupJsonString: String? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val semesterRepository: SemesterRepository,
    private val periodRepository: PeriodRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _isTesting = MutableStateFlow(false)
    val isTesting = _isTesting.asStateFlow()

    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage = _testMessage.asStateFlow()

    private val _showLlmDialog = MutableStateFlow(false)
    val showLlmDialog = _showLlmDialog.asStateFlow()

    private val _showSurvivalDialog = MutableStateFlow(false)
    val showSurvivalDialog = _showSurvivalDialog.asStateFlow()

    private val _showLanguageDialog = MutableStateFlow(false)
    val showLanguageDialog = _showLanguageDialog.asStateFlow()

    private val _showClearConfirmDialog = MutableStateFlow(false)
    val showClearConfirmDialog = _showClearConfirmDialog.asStateFlow()

    private val _backupJson = MutableStateFlow<String?>(null)

    // Clean 5-parameter combine for repository preferences (NO vararg, avoids Kotlin reified array covariance bug)
    private val repoPreferencesFlow = combine(
        settingsRepository.apiKeyFlow,
        settingsRepository.baseUrlFlow,
        settingsRepository.modelFlow,
        settingsRepository.endWarnMinutesFlow,
        settingsRepository.appLanguageFlow
    ) { key, url, model, warn, lang ->
        SettingsUiState(
            apiKey = key,
            baseUrl = url,
            model = model,
            endWarnMinutes = warn,
            appLanguage = lang
        )
    }

    // Combine base repository preferences with transient dialog UI state
    val uiState: StateFlow<SettingsUiState> = combine(
        repoPreferencesFlow,
        _isTesting,
        _testMessage,
        _showLlmDialog,
        _showSurvivalDialog
    ) { base, testing, msg, llmDlg, survDlg ->
        base.copy(
            isTestingConnection = testing,
            testConnectionMessage = msg,
            showLlmDialog = llmDlg,
            showSurvivalDialog = survDlg
        )
    }.combine(_showLanguageDialog) { state, langDlg ->
        state.copy(showLanguageDialog = langDlg)
    }.combine(_showClearConfirmDialog) { state, clearDlg ->
        state.copy(showClearConfirmDialog = clearDlg)
    }.combine(_backupJson) { state, json ->
        state.copy(backupJsonString = json)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun openLlmDialog() {
        _testMessage.value = null
        _showLlmDialog.value = true
    }

    fun closeLlmDialog() {
        _showLlmDialog.value = false
    }

    fun openLanguageDialog() {
        _showLanguageDialog.value = true
    }

    fun closeLanguageDialog() {
        _showLanguageDialog.value = false
    }

    fun openSurvivalGuide() {
        _showSurvivalDialog.value = true
    }

    fun closeSurvivalGuide() {
        _showSurvivalDialog.value = false
    }

    fun setAppLanguage(lang: String) {
        viewModelScope.launch {
            settingsRepository.updateAppLanguage(lang)
            _showLanguageDialog.value = false
        }
    }

    fun saveLlmSettings(apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            settingsRepository.updateApiKey(apiKey.trim())
            settingsRepository.updateBaseUrl(baseUrl.trim())
            settingsRepository.updateModel(model.trim())
            closeLlmDialog()
        }
    }

    fun testLlmConnection(apiKey: String, baseUrl: String, model: String) {
        viewModelScope.launch {
            _isTesting.value = true
            _testMessage.value = "正在测试连接..."
            val client = LlmClient(baseUrl, apiKey, model)
            val result = client.testConnection()
            _isTesting.value = false
            _testMessage.value = result.getOrElse { it.message ?: "连接失败" }
        }
    }

    fun updateWarnMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateEndWarnMinutes(minutes)
        }
    }

    fun openClearConfirmDialog() {
        _showClearConfirmDialog.value = true
    }

    fun closeClearConfirmDialog() {
        _showClearConfirmDialog.value = false
    }

    fun clearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            backupManager.clearAllData()
            _showClearConfirmDialog.value = false
            onDone()
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupManager.exportToJson()
            _backupJson.value = json
            onResult(json)
        }
    }

    fun importBackup(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupManager.importFromJson(jsonString, ImportMode.REPLACE)
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
