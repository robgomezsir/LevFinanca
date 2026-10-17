package com.example.lev.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.lev.LevApplication
import com.example.lev.ui.navigation.StartDestinationViewModel
import com.example.lev.ui.screens.accounts.AccountsViewModel
import com.example.lev.ui.screens.budget.BudgetViewModel
import com.example.lev.ui.screens.categories.CategoriesViewModel
import com.example.lev.ui.screens.dashboard.DashboardViewModel
import com.example.lev.ui.screens.goals.GoalsViewModel
import com.example.lev.ui.screens.onboarding.OnboardingViewModel
import com.example.lev.ui.screens.recurring.RecurrencesViewModel
import com.example.lev.ui.screens.reports.ReportsViewModel
import com.example.lev.ui.screens.settings.SettingsViewModel
import com.example.lev.ui.screens.sync.SyncViewModel
import com.example.lev.ui.screens.transactions.TransactionsViewModel
import com.example.lev.ui.theme.ThemeViewModel

object AppViewModelProvider {
    val Factory =
        viewModelFactory {
            initializer {
                StartDestinationViewModel(
                    settingsRepository = levApplication().container.settingsRepository,
                )
            }
            initializer {
                OnboardingViewModel(
                    financeRepository = levApplication().container.financeRepository,
                    settingsRepository = levApplication().container.settingsRepository,
                    syncRepository = levApplication().container.syncRepository,
                )
            }
            initializer {
                TransactionsViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                DashboardViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                AccountsViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                CategoriesViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                ReportsViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                SettingsViewModel(
                    settingsRepository = levApplication().container.settingsRepository,
                )
            }
            initializer {
                BudgetViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                GoalsViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                RecurrencesViewModel(
                    financeRepository = levApplication().container.financeRepository,
                )
            }
            initializer {
                ThemeViewModel(
                    settingsRepository = levApplication().container.settingsRepository,
                )
            }
            initializer {
                SyncViewModel(
                    syncRepository = levApplication().container.syncRepository,
                )
            }
        }
}

/**
 * Extension function to queries for [LevApplication] object and returns an instance of
 * [LevApplication].
 */
fun CreationExtras.levApplication(): LevApplication =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LevApplication)
