package com.example.lev.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Decide a primeira tela a partir do onboarding persistido.
 *
 * A leitura é suspend, então o destino começa nulo: o host espera o valor em vez de exibir a
 * tela errada por um instante e navegar depois.
 */
class StartDestinationViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            _startDestination.value = resolveRoute(settingsRepository.isOnboardingComplete())
        }
    }

    private fun resolveRoute(onboardingComplete: Boolean): String =
        if (onboardingComplete) Screen.Main.route else Screen.Onboarding.route
}
