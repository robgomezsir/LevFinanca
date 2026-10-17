package com.example.lev.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.repository.SettingsRepository
import com.example.lev.ui.theme.APPEARANCE_KEY
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.DYNAMIC_COLOR_KEY
import com.example.lev.ui.theme.THEME_MODE_KEY
import com.example.lev.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val useBiometrics: Boolean = false,
    val accountRemindersEnabled: Boolean = false,
    val syncEnabled: Boolean = false,
    val syncServer: String = "",
    val syncToken: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appearance: Appearance = Appearance.SOLID,
    val dynamicColor: Boolean = false,
    val syncSheetsConfigured: Boolean = false,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val biometrics = settingsRepository.getSetting("use_biometrics") == "true"
            val reminders = settingsRepository.getSetting("account_reminders") == "true"
            val sync = settingsRepository.getSetting("sync_enabled") == "true"
            val server = settingsRepository.getSetting("sync_server") ?: ""
            val token = settingsRepository.getSetting("sync_token") ?: ""

            _uiState.value =
                SettingsUiState(
                    useBiometrics = biometrics,
                    accountRemindersEnabled = reminders,
                    syncEnabled = sync,
                    syncServer = server,
                    syncToken = token,
                    themeMode = ThemeMode.fromKey(settingsRepository.getSetting(THEME_MODE_KEY)),
                    appearance = Appearance.fromKey(settingsRepository.getSetting(APPEARANCE_KEY)),
                    dynamicColor = settingsRepository.getSetting(DYNAMIC_COLOR_KEY) == "true",
                    syncSheetsConfigured = !settingsRepository.getSetting("sync_sheets_url").isNullOrBlank(),
                )
        }
    }

    fun setUseBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetting("use_biometrics", enabled.toString())
            _uiState.value = _uiState.value.copy(useBiometrics = enabled)
        }
    }

    fun setAccountReminders(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetting("account_reminders", enabled.toString())
            _uiState.value = _uiState.value.copy(accountRemindersEnabled = enabled)
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetting("sync_enabled", enabled.toString())
            _uiState.value = _uiState.value.copy(syncEnabled = enabled)
        }
    }

    fun setSyncServer(server: String) {
        viewModelScope.launch {
            settingsRepository.saveSetting("sync_server", server)
            _uiState.value = _uiState.value.copy(syncServer = server)
        }
    }

    fun setSyncToken(token: String) {
        _uiState.value = _uiState.value.copy(syncToken = token)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.saveSetting(THEME_MODE_KEY, mode.key)
            _uiState.value = _uiState.value.copy(themeMode = mode)
        }
    }

    fun setAppearance(appearance: Appearance) {
        viewModelScope.launch {
            settingsRepository.saveSetting(APPEARANCE_KEY, appearance.key)
            _uiState.value = _uiState.value.copy(appearance = appearance)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveSetting(DYNAMIC_COLOR_KEY, enabled.toString())
            _uiState.value = _uiState.value.copy(dynamicColor = enabled)
        }
    }
}
