package com.example.lev.ui.screens.transactions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Transaction
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.MoneyText
import com.example.lev.ui.theme.ExpenseDark
import com.example.lev.ui.theme.ExpenseLight
import com.example.lev.ui.theme.IncomeDark
import com.example.lev.ui.theme.IncomeLight
import com.example.lev.ui.theme.LedgerLineDark
import com.example.lev.ui.theme.LedgerLineLight
import com.example.lev.ui.theme.PendingDark
import com.example.lev.ui.theme.PendingLight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionListScreen(
    transactions: List<Transaction>,
    accounts: List<Account>,
    categories: List<Category>,
    onNewTransaction: () -> Unit,
    onTransactionUpdate: (Transaction) -> Unit,
    onTransactionDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
    filterAccountId: String? = null,
    filterCategoryId: String? = null,
    onClearFilters: () -> Unit = {},
) {
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }

    val visibleTransactions = applyTxFilters(transactions, filterAccountId, filterCategoryId)
    val filterLabel = txFilterLabel(accounts, categories, filterAccountId, filterCategoryId)

    if (transactions.isEmpty()) {
        EmptyState(
            title = "Nenhum lançamento",
            description = "Toque em + para registrar seu primeiro lançamento.",
            modifier = modifier.fillMaxSize(),
            actionLabel = "Registrar",
            onAction = onNewTransaction,
        )
    } else {
        val grouped = visibleTransactions.groupBy { it.date }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            if (filterLabel != null) {
                item(key = "active-filter") {
                    ActiveFilterRow(label = filterLabel, onClear = onClearFilters)
                }
            }
            if (visibleTransactions.isEmpty()) {
                item(key = "empty-filter") {
                    EmptyState(
                        title = "Nada por aqui",
                        description = "Nenhum lançamento para este filtro.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    )
                }
            }
            grouped.forEach { (date, dailyTxs) ->
                stickyHeader {
                    DailyHeader(date = date, transactions = dailyTxs)
                }
                items(dailyTxs, key = { it.id }) { tx ->
                    TransactionItem(
                        transaction = tx,
                        accounts = accounts,
                        onClick = { selectedTransaction = tx },
                    )
                }
            }
        }
    }

    selectedTransaction?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            onDismiss = { selectedTransaction = null },
            onSave = onTransactionUpdate,
            onDelete = onTransactionDelete,
        )
    }
}

private fun applyTxFilters(
    transactions: List<Transaction>,
    accountId: String?,
    categoryId: String?,
): List<Transaction> {
    if (accountId == null && categoryId == null) return transactions
    return transactions.filter { tx ->
        (accountId == null || tx.accountId == accountId || tx.destinationAccountId == accountId) &&
            (categoryId == null || tx.categoryId == categoryId)
    }
}

private fun txFilterLabel(
    accounts: List<Account>,
    categories: List<Category>,
    accountId: String?,
    categoryId: String?,
): String? =
    when {
        accountId != null ->
            accounts.firstOrNull { it.id == accountId }?.name ?: "Conta"
        categoryId != null ->
            categories.firstOrNull { it.id == categoryId }?.name ?: "Categoria"
        else -> null
    }

@Composable
private fun ActiveFilterRow(
    label: String,
    onClear: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(
            onClick = onClear,
            label = { Text("$label · limpar") },
        )
    }
}

@Composable
fun DailyHeader(
    date: LocalDate,
    transactions: List<Transaction>,
) {
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))
    val totalCents =
        transactions.sumOf {
            if (it.type == "EXPENSE") {
                -it.amountCents
            } else if (it.type == "INCOME") {
                it.amountCents
            } else {
                0L
            }
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = date.format(dayFormatter).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        MoneyText(
            cents = totalCents,
            style = MaterialTheme.typography.labelLarge,
            useSemanticColor = true,
        )
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    accounts: List<Account>,
    onClick: () -> Unit,
) {
    val accountName = accounts.firstOrNull { it.id == transaction.accountId }?.name
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val statusColor = transactionStatusColor(transaction, dark)
    val subtitle = transactionSubtitle(transaction, accountName)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClickLabel = "${transaction.description}, $subtitle", onClick = onClick)
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .background(statusColor, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.description.ifBlank { "Sem descrição" },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MoneyText(
            cents = transactionDisplayAmount(transaction),
            style = MaterialTheme.typography.titleMedium,
            useSemanticColor = true,
            showSign = true,
        )
    }
    HorizontalDivider(
        thickness = 1.dp,
        color = if (dark) LedgerLineDark else LedgerLineLight,
    )
}

@Composable
private fun transactionStatusColor(
    transaction: Transaction,
    dark: Boolean,
): androidx.compose.ui.graphics.Color =
    when {
        transaction.status == "PENDING" && transaction.type == "EXPENSE" ->
            if (dark) PendingDark else PendingLight
        transaction.type == "EXPENSE" -> if (dark) ExpenseDark else ExpenseLight
        transaction.type == "INCOME" -> if (dark) IncomeDark else IncomeLight
        else -> MaterialTheme.colorScheme.outline
    }

private fun transactionSubtitle(
    transaction: Transaction,
    accountName: String?,
): String =
    buildList {
        add(accountName ?: "Sem conta")
        if (transaction.installmentTotal != null && transaction.installmentNumber != null) {
            add("${transaction.installmentNumber}/${transaction.installmentTotal}")
        }
        if (transaction.status == "PENDING") add("Pendente")
    }.joinToString(" · ")

private fun transactionDisplayAmount(transaction: Transaction): Long =
    when (transaction.type) {
        "EXPENSE" -> -transaction.amountCents
        "INCOME" -> transaction.amountCents
        else -> 0L
    }
