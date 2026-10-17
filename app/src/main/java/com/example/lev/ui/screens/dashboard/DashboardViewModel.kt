package com.example.lev.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Account
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.AccountBalances
import com.example.lev.domain.CreditCardSummaryCalculator
import com.example.lev.domain.InvoiceGenerator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CardSummaryUiState(
    val cards: List<CardSummaryItem> = emptyList(),
)

data class InvoiceHistoryItem(
    val monthLabel: String,
    val totalCents: Long,
    val dueDateLabel: String,
)

data class CardSummaryItem(
    val account: Account,
    /** Saldo real (Base §5.3): inicial + entradas − saídas ± transferências PAID. */
    val currentBalanceCents: Long = 0L,
    val usedCents: Long = 0L,
    val availableCents: Long = 0L,
    val futureInstallmentsCents: Long = 0L,
    /** Total da fatura do ciclo corrente (0 se paga ou sem compras). */
    val currentInvoiceTotalCents: Long = 0L,
    val invoiceHistory: List<InvoiceHistoryItem> = emptyList(),
)

class DashboardViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<CardSummaryUiState> =
        combine(
            financeRepository.getAccounts(),
            financeRepository.getTransactions(),
            financeRepository.getAllInvoices(),
        ) { accounts, transactions, invoices ->
            val today = LocalDate.now()
            val cards =
                accounts.map { account ->
                    if (account.type == "CREDIT_CARD") {
                        cardItem(account, transactions, invoices, today)
                    } else {
                        val balance = AccountBalances.currentBalance(account, transactions)
                        CardSummaryItem(
                            account = account,
                            currentBalanceCents = balance,
                            availableCents = balance,
                        )
                    }
                }

            CardSummaryUiState(cards)
        }.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = CardSummaryUiState(),
        )

    private fun cardItem(
        account: Account,
        transactions: List<com.example.lev.data.model.Transaction>,
        invoices: List<com.example.lev.data.model.Invoice>,
        today: LocalDate,
    ): CardSummaryItem {
        val drafts = InvoiceGenerator.generateForAccount(account, transactions)
        val currentRef = AccountBalances.currentCycleReference(account, today)
        val paid = AccountBalances.paidMonths(account.id, invoices)
        val currentTotal =
            drafts
                .firstOrNull { it.referenceMonth == currentRef && it.referenceMonth !in paid }
                ?.totalCents ?: 0L
        val openTotal = AccountBalances.cardOpenTotal(account, transactions, invoices)
        val futureTotal = AccountBalances.cardFutureTotal(account, transactions, invoices, today)
        val limit = account.creditLimitCents ?: 0L

        val invoiceHistory =
            drafts
                .sortedByDescending { it.referenceMonth }
                .take(3)
                .map { draft ->
                    val dueDate = draft.toInvoice().dueDate
                    InvoiceHistoryItem(
                        monthLabel = "${draft.referenceMonth.monthValue}/${draft.referenceMonth.year}",
                        totalCents = draft.totalCents,
                        dueDateLabel = dueDate.toString(),
                    )
                }
        return CardSummaryItem(
            account = account,
            usedCents = openTotal,
            availableCents = CreditCardSummaryCalculator.availableCredit(limit, openTotal),
            futureInstallmentsCents = futureTotal,
            currentInvoiceTotalCents = currentTotal,
            invoiceHistory = invoiceHistory,
        )
    }
}
