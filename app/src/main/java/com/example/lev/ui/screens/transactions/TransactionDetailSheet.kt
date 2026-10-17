package com.example.lev.ui.screens.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Transaction
import com.example.lev.domain.MoneyFormatter
import com.example.lev.domain.MoneyParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: Transaction,
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit,
    onDelete: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var description by remember { mutableStateOf(transaction.description) }
    var amountCents by remember { mutableStateOf(transaction.amountCents) }
    var amountText by remember { mutableStateOf(MoneyFormatter.formatNumberOnly(transaction.amountCents)) }
    var status by remember { mutableStateOf(transaction.status) }
    var selectedCategoryId by remember { mutableStateOf(transaction.categoryId) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val accountName = accounts.firstOrNull { it.id == transaction.accountId }?.name ?: transaction.accountId
    val categoryKind = if (transaction.type == "INCOME") "INCOME" else "EXPENSE"
    val availableCategories = categories.filter { it.kind == categoryKind }
    val installmentLabel =
        if (transaction.installmentTotal != null && transaction.installmentNumber != null) {
            "Parcela ${transaction.installmentNumber}/${transaction.installmentTotal}"
        } else {
            null
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Detalhes do lançamento",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                text = "$accountName • ${transaction.date}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (installmentLabel != null) {
                Text(
                    text = installmentLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { text ->
                    amountText = text
                    val parsed = MoneyParser.parse(text)
                    amountError = parsed <= 0L
                    if (!amountError) amountCents = parsed
                },
                label = { Text("Valor (R$)") },
                isError = amountError,
                supportingText = if (amountError) ({ Text("Informe um valor válido") }) else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(text = "Status", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = status == "PENDING",
                    onClick = { status = "PENDING" },
                    label = { Text("Pendente") },
                )
                FilterChip(
                    selected = status == "PAID",
                    onClick = { status = "PAID" },
                    label = { Text("Pago") },
                )
            }

            Text(text = "Categoria", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { selectedCategoryId = null },
                    label = { Text("Sem categoria") },
                )
                availableCategories.take(4).forEach { category ->
                    FilterChip(
                        selected = category.id == selectedCategoryId,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        if (amountError || amountCents <= 0L) return@Button
                        onSave(
                            transaction.copy(
                                description = description.ifBlank { transaction.description },
                                amountCents = amountCents,
                                status = status,
                                categoryId = selectedCategoryId,
                            ),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Salvar")
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.weight(1f),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                ) {
                    Text("Excluir")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir lançamento?") },
            text = { Text("Esta ação remove o lançamento de \"${transaction.description}\".") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(transaction.id)
                        onDismiss()
                    },
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
