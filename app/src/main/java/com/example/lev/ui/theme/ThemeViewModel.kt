package com.example.lev.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ThemeUiState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val appearance: Appearance = Appearance.SOLID,
    val dynamicColor: Boolean = false,
)

class ThemeViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<ThemeUiState> =
        combine(
            settingsRepository.observeSetting(THEME_MODE_KEY),
            settingsRepository.observeSetting(APPEARANCE_KEY),
            settingsRepository.observeSetting(DYNAMIC_COLOR_KEY),
        ) { mode, appearance, dynamic ->
            ThemeUiState(
                mode = ThemeMode.fromKey(mode),
                appearance = Appearance.fromKey(appearance),
                dynamicColor = dynamic == "true",
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeUiState(),
        )
}
