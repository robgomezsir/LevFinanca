package com.example.lev.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.local.SeedData
import com.example.lev.data.model.Account
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.data.repository.SettingsRepository
import com.example.lev.data.repository.SyncRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class OnboardingViewModel(
    private val financeRepository: FinanceRepository,
    private val settingsRepository: SettingsRepository,
    private val syncRepository: SyncRepository? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun nextStep() {
        val currentStep = _uiState.value.step
        if (currentStep < 3) {
            _uiState.value = _uiState.value.copy(step = currentStep + 1)
        }
    }

    fun finishOnboarding(
        accountName: String,
        initialBalance: Long,
        sheetsUrl: String = "",
        sheetsSecret: String = "",
    ) {
        viewModelScope.launch {
            // 1. Seed categories
            SeedData.defaultCategories.forEach { category ->
                financeRepository.saveCategory(category)
            }

            // 2. Create first account
            val now = System.currentTimeMillis()
            val account =
                Account(
                    id = UUID.randomUUID().toString(),
                    name = accountName.ifBlank { "Minha Conta" },
                    type = "CHECKING",
                    initialBalanceCents = initialBalance,
                    // Primary brand color.
                    color = "#006C5B",
                    icon = "account_balance_wallet",
                    createdAt = now,
                    updatedAt = now,
                )
            financeRepository.saveAccount(account)

            // 3. Planilha (opcional, pulável): salva URL + segredo cifrado.
            if (sheetsUrl.isNotBlank() && sheetsSecret.isNotBlank()) {
                runCatching { syncRepository?.saveConnection(sheetsUrl, sheetsSecret) }
            }

            // 4. Mark onboarding complete
            settingsRepository.setOnboardingComplete()

            _uiState.value = _uiState.value.copy(isFinished = true)
        }
    }
}

data class OnboardingUiState(
    val step: Int = 0, // 0 = Welcome, 1 = Categories Tour, 2 = Create Account, 3 = Connect Sheets
    val isFinished: Boolean = false,
)
