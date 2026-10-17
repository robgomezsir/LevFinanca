package com.example.lev.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Account
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.AccountBalances
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    /** Saldo real por conta (Base §5.3); cartão = fatura em aberto. */
    val balances: Map<String, Long> = emptyMap(),
    val isLoading: Boolean = true,
)

class AccountsViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<AccountsUiState> =
        combine(
            financeRepository.getAccounts(),
            financeRepository.getTransactions(),
            financeRepository.getAllInvoices(),
        ) { accounts, transactions, invoices ->
            val balances =
                accounts.associate { account ->
                    account.id to
                        if (account.type == "CREDIT_CARD") {
                            AccountBalances.cardOpenTotal(account, transactions, invoices)
                        } else {
                            AccountBalances.currentBalance(account, transactions)
                        }
                }
            AccountsUiState(accounts = accounts, balances = balances, isLoading = false)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountsUiState(isLoading = true),
        )

    val accountTypes = listOf("CHECKING", "SAVINGS", "CASH", "CREDIT_CARD", "INVESTMENT", "BENEFIT", "LOAN")

    val accountTypeLabels =
        mapOf(
            "CHECKING" to "Conta corrente",
            "SAVINGS" to "Poupança",
            "CASH" to "Dinheiro",
            "CREDIT_CARD" to "Cartão de crédito",
            "INVESTMENT" to "Investimento",
            "BENEFIT" to "Benefício",
            "LOAN" to "Empréstimo",
        )

    fun createAccount(
        name: String,
        type: String,
        initialBalanceCents: Long = 0L,
        color: String = "#006C5B",
        icon: String = "account_balance_wallet",
        creditLimitCents: Long? = null,
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val account =
                Account(
                    id = java.util.UUID.randomUUID().toString(),
                    name = name,
                    type = type,
                    initialBalanceCents = initialBalanceCents,
                    color = color,
                    icon = icon,
                    creditLimitCents = creditLimitCents,
                    createdAt = now,
                    updatedAt = now,
                )
            financeRepository.saveAccount(account)
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            financeRepository.deleteAccount(id)
        }
    }
}
