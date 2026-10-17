package com.example.lev.ui.screens.invoices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.model.Account
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.domain.InvoiceCycle
import com.example.lev.domain.InvoiceGenerator
import com.example.lev.domain.InvoiceLifecycle
import com.example.lev.domain.MoneyFormatter
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.screens.transactions.TransactionDetailSheet
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

private data class InvoiceUiState(
    val invoiceRef: InvoiceCycle.InvoiceRef,
    val currentInvoice: Invoice,
    val invoiceTransactions: List<Transaction>,
    val total: Long,
    val isPayable: Boolean,
    val canReopen: Boolean,
)

private fun buildInvoiceUiState(
    account: Account,
    transactions: List<Transaction>,
    currentDate: LocalDate,
): InvoiceUiState {
    val invoiceRef =
        InvoiceCycle.calculateInvoiceForPurchase(
            purchaseDate = currentDate,
            closingDay = account.closingDay ?: 25,
            dueDay = account.dueDay ?: 5,
            closingInclusive = account.closingInclusive,
        )

    val generatedInvoice =
        InvoiceGenerator.generateForAccount(account, transactions)
            .firstOrNull { it.referenceMonth == invoiceRef.referenceMonth }
            ?.toInvoice()

    val invoiceTransactions =
        transactions.filter {
            it.accountId == account.id &&
                it.type == "EXPENSE" &&
                it.status == "PAID" &&
                it.date.year == invoiceRef.referenceMonth.year &&
                it.date.month == invoiceRef.referenceMonth.month
        }
    val total = generatedInvoice?.totalCents ?: invoiceTransactions.sumOf { it.amountCents }
    val currentInvoiceStatus = generatedInvoice?.status ?: "OPEN"
    val invoiceId =
        "invoice-${account.id}-${invoiceRef.referenceMonth.year}-${invoiceRef.referenceMonth.monthValue}"
    val currentInvoice =
        generatedInvoice?.copy(
            id = invoiceId,
            accountId = account.id,
            status = currentInvoiceStatus,
        ) ?: Invoice(
            id = invoiceId,
            accountId = account.id,
            referenceMonth = invoiceRef.referenceMonth,
            closingDate = invoiceRef.closingDate,
            dueDate = invoiceRef.dueDate,
            status = currentInvoiceStatus,
            totalCents = total,
            paidAmountCents = 0L,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )

    return InvoiceUiState(
        invoiceRef = invoiceRef,
        currentInvoice = currentInvoice,
        invoiceTransactions = invoiceTransactions,
        total = total,
        isPayable = InvoiceLifecycle.isPayable(currentInvoice),
        canReopen = InvoiceLifecycle.canReopen(currentInvoice),
    )
}

@Composable
fun InvoiceDetailsScreen(
    financeRepository: FinanceRepository,
    accountId: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val accounts by financeRepository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val transactions by financeRepository.getTransactions().collectAsStateWithLifecycle(initialValue = emptyList())
    val categories by financeRepository.getCategories().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var selectedTransaction by rememberSaveable { mutableStateOf<String?>(null) }

    val account = accounts.firstOrNull { it.id == accountId }
    if (account == null) {
        Text("Cartão não encontrado", modifier = modifier)
        return
    }

    val currentDate = LocalDate.now()
    val uiState = buildInvoiceUiState(account, transactions, currentDate)
    val invoiceRef = uiState.invoiceRef
    val currentInvoice = uiState.currentInvoice
    val invoiceTransactions = uiState.invoiceTransactions
    val total = uiState.total
    val isPayable = uiState.isPayable
    val canReopen = uiState.canReopen
    val annualPaymentState = InvoiceLifecycle.remainingBalance(currentInvoice)

    val paymentAccountId =
        account.paymentAccountId
            ?: accounts.firstOrNull { it.type in listOf("CHECKING", "SAVINGS", "CASH") }?.id

    val invoiceId = currentInvoice.id

    fun payInvoice() {
        if (paymentAccountId == null || total <= 0L || !isPayable) return

        val now = System.currentTimeMillis()
        val baseInvoice =
            currentInvoice.copy(
                id = invoiceId,
                accountId = accountId,
                updatedAt = now,
            )

        val paymentResult = InvoiceLifecycle.pay(baseInvoice, total)
        val transfer =
            Transaction(
                id = UUID.randomUUID().toString(),
                type = "TRANSFER",
                description = "Pagamento de fatura",
                amountCents = total,
                date = currentDate,
                status = "PAID",
                accountId = paymentAccountId,
                destinationAccountId = accountId,
                invoiceId = invoiceId,
                createdAt = now,
                updatedAt = now,
            )

        scope.launch {
            financeRepository.saveTransaction(transfer)
            financeRepository.saveInvoice(paymentResult.invoice)
        }
    }

    fun reopenInvoice() {
        if (!canReopen) return
        scope.launch {
            financeRepository.saveInvoice(InvoiceLifecycle.reopen(currentInvoice))
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text(account.name) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                InvoiceHeaderCard(
                    accountName = account.name,
                    referenceLabel = "Fatura ${invoiceRef.referenceMonth.monthValue}/${invoiceRef.referenceMonth.year}",
                    closingDate = invoiceRef.closingDate.toString(),
                    dueDate = invoiceRef.dueDate.toString(),
                    total = total,
                    remaining = annualPaymentState,
                    showPayAction = paymentAccountId != null && isPayable,
                    showReopenAction = canReopen,
                    isSettled = paymentAccountId != null && total > 0L,
                    onPay = { payInvoice() },
                    onReopen = { reopenInvoice() },
                )
            }

            item {
                Text(
                    text = "Lançamentos",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (invoiceTransactions.isEmpty()) {
                item {
                    Text(
                        text = "Nenhum lançamento nesta fatura.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(invoiceTransactions, key = { it.id }) { tx ->
                    InvoiceRow(tx, onClick = { selectedTransaction = tx.id })
                }
            }
        }
    }

    invoiceTransactions.firstOrNull { it.id == selectedTransaction }?.let { tx ->
        TransactionDetailSheet(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            onDismiss = { selectedTransaction = null },
            onSave = { updated ->
                scope.launch { financeRepository.updateTransaction(updated) }
                selectedTransaction = null
            },
            onDelete = { id ->
                scope.launch { financeRepository.deleteTransaction(id) }
                selectedTransaction = null
            },
        )
    }
}

@Composable
private fun InvoiceHeaderCard(
    accountName: String,
    referenceLabel: String,
    closingDate: String,
    dueDate: String,
    total: Long,
    remaining: Long,
    showPayAction: Boolean,
    showReopenAction: Boolean,
    isSettled: Boolean,
    onPay: () -> Unit,
    onReopen: () -> Unit,
) {
    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = accountName,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = referenceLabel,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            InvoiceInfoRow(label = "Fechamento", value = closingDate)
            InvoiceInfoRow(label = "Vencimento", value = dueDate)
            InvoiceInfoRow(label = "Total", value = MoneyFormatter.format(total))
            if (remaining > 0L) {
                InvoiceInfoRow(label = "Saldo restante", value = MoneyFormatter.format(remaining))
            }
            when {
                showPayAction -> InvoiceActionButton(label = "Pagar fatura", onClick = onPay)
                showReopenAction -> InvoiceActionButton(label = "Reabrir fatura", onClick = onReopen)
                isSettled -> InvoiceActionButton(label = "Fatura paga", onClick = null)
            }
        }
    }
}

@Composable
private fun InvoiceInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Text(value)
    }
}

@Composable
private fun InvoiceActionButton(
    label: String,
    onClick: (() -> Unit)?,
) {
    Button(
        onClick = { onClick?.invoke() },
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null,
    ) {
        Text(label)
    }
}

@Composable
private fun InvoiceRow(
    tx: Transaction,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(tx.description)
            Text(
                text = tx.date.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(MoneyFormatter.format(tx.amountCents))
    }
    Spacer(modifier = Modifier.height(4.dp))
}
