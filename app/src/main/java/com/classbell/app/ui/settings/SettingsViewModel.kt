package com.classbell.app.ui.settings

import android.content.Context
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
    val isTestingConnection: Boolean = false,
    val testConnectionMessage: String? = null,
    val showLlmDialog: Boolean = false,
    val showSurvivalDialog: Boolean = false,
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
    private val _testMessage = MutableStateFlow<String?>(null)
    private val _showLlmDialog = MutableStateFlow(false)
    private val _showSurvivalDialog = MutableStateFlow(false)
    private val _showClearConfirmDialog = MutableStateFlow(false)
    private val _backupJson = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.apiKeyFlow,
        settingsRepository.baseUrlFlow,
        settingsRepository.modelFlow,
        settingsRepository.endWarnMinutesFlow,
        _isTesting,
        _testMessage,
        _showLlmDialog,
        _showSurvivalDialog,
        _showClearConfirmDialog,
        _backupJson
    ) { params ->
        SettingsUiState(
            apiKey = params[0] as String,
            baseUrl = params[1] as String,
            model = params[2] as String,
            endWarnMinutes = params[3] as Int,
            isTestingConnection = params[4] as Boolean,
            testConnectionMessage = params[5] as String?,
            showLlmDialog = params[6] as Boolean,
            showSurvivalDialog = params[7] as Boolean,
            showClearConfirmDialog = params[8] as Boolean,
            backupJsonString = params[9] as String?
        )
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

    fun openSurvivalGuide() {
        _showSurvivalDialog.value = true
    }

    fun closeSurvivalGuide() {
        _showSurvivalDialog.value = false
    }

    fun openClearConfirm() {
        _showClearConfirmDialog.value = true
    }

    fun closeClearConfirm() {
        _showClearConfirmDialog.value = false
    }

    fun clearAllData() {
        viewModelScope.launch {
            backupManager.clearAllData()
            closeClearConfirm()
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupManager.exportToJson()
            onReady(json)
        }
    }

    fun importBackup(jsonString: String, mode: ImportMode, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val res = backupManager.importFromJson(jsonString, mode)
            if (res.isSuccess) {
                onResult(true, "导入成功")
            } else {
                onResult(false, res.exceptionOrNull()?.message ?: "导入失败")
            }
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
