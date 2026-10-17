package com.example.lev.ui.screens.transactions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.ui.components.AmountKeypad

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastEntryBottomSheet(
    accounts: List<Account>,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (FastEntryResult) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("R$ 0,00") }
    var transactionType by remember { mutableStateOf("EXPENSE") }
    var description by remember { mutableStateOf("") }
    var installments by remember { mutableStateOf(1) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showAccountError by remember { mutableStateOf(false) }

    val availableAccounts = accounts.filter { transactionType != "TRANSFER" || it.type != "CREDIT_CARD" }
    val selectedAccount = availableAccounts.firstOrNull { it.id == selectedAccountId }
    val isCard = selectedAccount?.type == "CREDIT_CARD"
    val categoryKind = if (transactionType == "INCOME") "INCOME" else "EXPENSE"
    val availableCategories = categories.filter { it.kind == categoryKind }

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
                    .padding(bottom = 24.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FilterChip(
                    selected = transactionType == "EXPENSE",
                    onClick = {
                        transactionType = "EXPENSE"
                        installments = 1
                    },
                    label = { Text("Despesa") },
                )
                FilterChip(
                    selected = transactionType == "INCOME",
                    onClick = {
                        transactionType = "INCOME"
                        installments = 1
                    },
                    label = { Text("Receita") },
                )
                FilterChip(
                    selected = transactionType == "TRANSFER",
                    onClick = {
                        transactionType = "TRANSFER"
                        installments = 1
                    },
                    label = { Text("Transferência") },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = amountText,
                style = MaterialTheme.typography.displayMedium,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountSelector(
                    accounts = availableAccounts,
                    selectedAccountId = selectedAccountId,
                    onSelected = { account ->
                        selectedAccountId = account.id
                        showAccountError = false
                        if (account.type != "CREDIT_CARD") installments = 1
                    },
                    isError = showAccountError,
                )

                CategorySelector(
                    categories = availableCategories,
                    selectedCategoryId = selectedCategoryId,
                    onSelected = { selectedCategoryId = it },
                )

                if (transactionType == "EXPENSE") {
                    InstallmentsSelector(
                        enabled = isCard,
                        installments = installments,
                        onSelected = { installments = it },
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AmountKeypad(
                onDigit = { digit ->
                    amountText = if (amountText == "R$ 0,00") digit else amountText + digit
                },
                onOperator = { op -> amountText += op },
                onBackspace = {
                    amountText = if (amountText.length > 1) amountText.dropLast(1) else "R$ 0,00"
                },
                onSubmit = {
                    if (selectedAccountId.isBlank()) {
                        showAccountError = true
                        return@AmountKeypad
                    }
                    onSave(
                        FastEntryResult(
                            amountString = amountText,
                            type = transactionType,
                            accountId = selectedAccountId,
                            categoryId = selectedCategoryId,
                            installments = if (isCard) installments else 1,
                            description = description.trim(),
                        ),
                    )
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun AccountSelector(
    accounts: List<Account>,
    selectedAccountId: String,
    onSelected: (Account) -> Unit,
    isError: Boolean,
) {
    Text(text = "Conta", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val shown = accounts.take(4)
        shown.forEach { account ->
            FilterChip(
                selected = account.id == selectedAccountId,
                onClick = { onSelected(account) },
                label = { Text(account.name) },
            )
        }
        if (shown.isEmpty()) {
            Text(
                text = "Nenhuma conta disponível",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (isError) {
        Text(
            text = "Selecione uma conta",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun CategorySelector(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelected: (String?) -> Unit,
) {
    Text(text = "Categoria", style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onSelected(null) },
            label = { Text("Sem categoria") },
        )
        categories.take(4).forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onSelected(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstallmentsSelector(
    enabled: Boolean,
    installments: Int,
    onSelected: (Int) -> Unit,
) {
    Text(
        text = "Parcelas (apenas cartão)",
        style = MaterialTheme.typography.labelLarge,
        color =
            if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            },
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(1, 2, 3, 6, 12).forEach { count ->
            FilterChip(
                selected = installments == count,
                enabled = enabled,
                onClick = { onSelected(count) },
                label = { Text(if (count == 1) "1x" else "${count}x") },
            )
        }
    }
}
