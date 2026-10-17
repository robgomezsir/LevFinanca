package com.example.lev.ui.screens.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Recurrence
import com.example.lev.data.model.Transaction
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.RecurrenceEngine
import com.example.lev.domain.RecurrenceFrequency
import com.example.lev.domain.RecurrenceOccurrence
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.UUID

data class RecurrenceListItem(
    val recurrence: Recurrence,
    val nextOccurrences: List<RecurrenceOccurrence>,
    val accountName: String,
    val categoryName: String?,
)

data class RecurrencesUiState(
    val isLoading: Boolean = true,
    val recurrences: List<RecurrenceListItem> = emptyList(),
    val pausedRecurrences: List<RecurrenceListItem> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
)

/** Ativas e pausadas separadas: pausada também precisa continuar na tela, para poder ser retomada. */
data class RecurrenceLists(
    val active: List<RecurrenceListItem> = emptyList(),
    val paused: List<RecurrenceListItem> = emptyList(),
)

internal fun buildRecurrenceLists(
    recurrences: List<Recurrence>,
    accounts: List<Account>,
    categories: List<Category>,
    transactions: List<Transaction>,
    today: LocalDate,
): RecurrenceLists {
    val sorted = recurrences.sortedBy { it.startDate }
    return RecurrenceLists(
        active = sorted.filter { it.active }.map { toListItem(it, accounts, categories, transactions, today) },
        paused = sorted.filterNot { it.active }.map { toListItem(it, accounts, categories, transactions, today) },
    )
}

private fun toListItem(
    recurrence: Recurrence,
    accounts: List<Account>,
    categories: List<Category>,
    transactions: List<Transaction>,
    today: LocalDate,
): RecurrenceListItem {
    val upcoming =
        RecurrenceEngine.generate(
            recurrenceId = recurrence.id,
            startDate = recurrence.startDate,
            frequency = RecurrencesViewModel.parseFrequency(recurrence.frequency),
            interval = recurrence.interval,
            horizonMonths = 3,
            amountCents = recurrence.amountCents,
            endDate = recurrence.endDate,
            maxOccurrences = recurrence.maxOccurrences,
            isVariable = recurrence.isVariable,
        ).filter { occurrence ->
            !occurrence.date.isBefore(today) &&
                transactions.none { tx ->
                    tx.recurrenceId == recurrence.id && tx.date == occurrence.date
                }
        }.take(3)

    return RecurrenceListItem(
        recurrence = recurrence,
        nextOccurrences = upcoming,
        accountName = accounts.firstOrNull { it.id == recurrence.accountId }?.name ?: "—",
        categoryName = categories.firstOrNull { it.id == recurrence.categoryId }?.name,
    )
}

class RecurrencesViewModel(
    private val financeRepository: FinanceRepository,
) : ViewModel() {
    val uiState: StateFlow<RecurrencesUiState> =
        combine(
            financeRepository.getRecurrences(),
            financeRepository.getAccounts(),
            financeRepository.getCategories(),
            financeRepository.getTransactions(),
        ) { recurrences, accounts, categories, transactions ->
            val lists =
                buildRecurrenceLists(
                    recurrences = recurrences,
                    accounts = accounts,
                    categories = categories,
                    transactions = transactions,
                    today = LocalDate.now(),
                )
            RecurrencesUiState(
                isLoading = false,
                recurrences = lists.active,
                pausedRecurrences = lists.paused,
                accounts = accounts,
                categories = categories,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RecurrencesUiState(),
        )

    fun createRecurrence(
        type: String,
        description: String,
        amountCents: Long,
        isVariable: Boolean,
        accountId: String,
        categoryId: String?,
        frequency: String,
        interval: Int,
        startDate: LocalDate,
    ) {
        viewModelScope.launch {
            if (amountCents <= 0L && !isVariable) return@launch
            val now = System.currentTimeMillis()
            financeRepository.saveRecurrence(
                Recurrence(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    description = description,
                    amountCents = if (isVariable) null else amountCents,
                    isVariable = isVariable,
                    accountId = accountId,
                    categoryId = categoryId,
                    frequency = frequency,
                    interval = interval.coerceAtLeast(1),
                    startDate = startDate,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    /** Materializes one occurrence into a real transaction. */
    fun confirmOccurrence(
        recurrence: Recurrence,
        occurrence: RecurrenceOccurrence,
    ) {
        viewModelScope.launch {
            val amount = occurrence.amountCents ?: recurrence.amountCents ?: 0L
            val now = System.currentTimeMillis()
            financeRepository.saveTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = recurrence.type,
                    description = recurrence.description,
                    amountCents = amount,
                    date = occurrence.date,
                    status = "PAID",
                    accountId = recurrence.accountId,
                    categoryId = recurrence.categoryId,
                    payee = recurrence.payee,
                    notes = recurrence.notes,
                    recurrenceId = recurrence.id,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
    }

    fun toggleActive(recurrence: Recurrence) {
        viewModelScope.launch {
            financeRepository.updateRecurrence(
                recurrence.copy(active = !recurrence.active, updatedAt = System.currentTimeMillis()),
            )
        }
    }

    fun deleteRecurrence(id: String) {
        viewModelScope.launch {
            financeRepository.deleteRecurrence(id)
        }
    }

    companion object {
        fun parseFrequency(value: String): RecurrenceFrequency =
            try {
                RecurrenceFrequency.valueOf(value)
            } catch (_: IllegalArgumentException) {
                RecurrenceFrequency.MONTHLY
            }

        fun parseDate(value: String): LocalDate? =
            try {
                LocalDate.parse(value)
            } catch (_: DateTimeParseException) {
                null
            }
    }
}
