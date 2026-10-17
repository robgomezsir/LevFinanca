package com.example.lev.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Transaction
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.MoneyParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

class TransactionsViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    // Fetch all active transactions from the DB and expose as StateFlow
    val transactions: StateFlow<List<Transaction>> =
        financeRepository.getTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val accounts: StateFlow<List<Account>> =
        financeRepository.getAccounts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    val categories: StateFlow<List<Category>> =
        financeRepository.getCategories()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /** Filtro aplicado ao tocar numa conta/categoria em outra tela. */
    private val _accountFilter = MutableStateFlow<String?>(null)
    val accountFilter: StateFlow<String?> = _accountFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    fun setAccountFilter(accountId: String?) {
        _accountFilter.value = accountId
        if (accountId != null) _categoryFilter.value = null
    }

    fun setCategoryFilter(categoryId: String?) {
        _categoryFilter.value = categoryId
        if (categoryId != null) _accountFilter.value = null
    }

    fun clearFilters() {
        _accountFilter.value = null
        _categoryFilter.value = null
    }

    fun saveFastEntry(entry: FastEntryResult) {
        viewModelScope.launch {
            val amountCents = MoneyParser.parse(entry.amountString)
            if (amountCents <= 0L) return@launch // Don't save empty/zero transactions

            if (entry.type == "EXPENSE" && entry.installments > 1) {
                financeRepository.saveCardPurchase(
                    description = entry.description.ifBlank { "Novo Lançamento" },
                    amountCents = amountCents,
                    accountId = entry.accountId,
                    installments = entry.installments,
                    categoryId = entry.categoryId,
                    date = LocalDate.now(),
                    status = "PAID",
                )
                return@launch
            }

            val now = System.currentTimeMillis()
            val transaction =
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = entry.type,
                    description =
                        entry.description.ifBlank {
                            if (entry.type == "TRANSFER") "Transferência" else "Novo Lançamento"
                        },
                    amountCents = amountCents,
                    date = LocalDate.now(),
                    status = "PAID",
                    accountId = entry.accountId,
                    categoryId = entry.categoryId,
                    createdAt = now,
                    updatedAt = now,
                )

            financeRepository.saveTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            financeRepository.updateTransaction(transaction.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            financeRepository.deleteTransaction(id)
        }
    }
}
