package com.example.lev.ui.screens.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.domain.MoneyParser
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LoadingList
import com.example.lev.ui.components.MoneyText

@Composable
fun RecurrencesScreen(
    viewModel: RecurrencesViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Recorrências",
                style = MaterialTheme.typography.titleLarge,
            )
            TextButton(onClick = { showCreate = true }) {
                Text("+ Nova")
            }
        }

        if (uiState.isLoading) {
            LoadingList()
        } else if (uiState.recurrences.isEmpty() && uiState.pausedRecurrences.isEmpty()) {
            EmptyState(
                title = "Nenhuma recorrência",
                description =
                    "Configure aluguel, salário, assinaturas e outras contas " +
                        "que se repetem para nunca esquecer.",
                actionLabel = "Criar recorrência",
                onAction = { showCreate = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(uiState.recurrences, key = { it.recurrence.id }) { item ->
                    RecurrenceCard(
                        item = item,
                        onConfirm = { occurrence ->
                            viewModel.confirmOccurrence(item.recurrence, occurrence)
                        },
                        onToggle = { viewModel.toggleActive(item.recurrence) },
                        onDelete = { viewModel.deleteRecurrence(item.recurrence.id) },
                    )
                }

                if (uiState.pausedRecurrences.isNotEmpty()) {
                    item(key = "paused-header") {
                        Text(
                            text = "Pausadas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    items(uiState.pausedRecurrences, key = { it.recurrence.id }) { item ->
                        RecurrenceCard(
                            item = item,
                            onConfirm = { occurrence ->
                                viewModel.confirmOccurrence(item.recurrence, occurrence)
                            },
                            onToggle = { viewModel.toggleActive(item.recurrence) },
                            onDelete = { viewModel.deleteRecurrence(item.recurrence.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateRecurrenceDialog(
            accounts = uiState.accounts.filter { it.type != "CREDIT_CARD" },
            onDismiss = { showCreate = false },
            onSave = {
                    type, description, amountCents, isVariable, accountId, categoryId, frequency, interval, startDate ->
                viewModel.createRecurrence(
                    type, description, amountCents, isVariable,
                    accountId, categoryId, frequency, interval, startDate,
                )
                showCreate = false
            },
        )
    }
}

@Composable
private fun RecurrenceCard(
    item: RecurrenceListItem,
    onConfirm: (com.example.lev.domain.RecurrenceOccurrence) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    val recurrence = item.recurrence
    val typeLabel = if (recurrence.type == "INCOME") "Receita" else "Despesa"
    val frequencyLabel =
        when (recurrence.frequency) {
            "DAILY" -> "Diária"
            "WEEKLY" -> "Semanal"
            "MONTHLY" -> "Mensal"
            "YEARLY" -> "Anual"
            else -> recurrence.frequency
        }

    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recurrence.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            "$typeLabel • $frequencyLabel • ${item.accountName}" +
                                (item.categoryName?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (recurrence.amountCents != null) {
                    MoneyText(
                        cents = if (recurrence.type == "INCOME") recurrence.amountCents else -recurrence.amountCents,
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else if (recurrence.isVariable) {
                    Text("Variável", style = MaterialTheme.typography.labelMedium)
                }
            }

            UpcomingOccurrences(
                occurrences = item.nextOccurrences,
                onConfirm = onConfirm,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Bug documentado no audit: o rótulo era sempre "Ativa".
                    Text(
                        text = if (recurrence.active) "Ativa" else "Pausada",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(checked = recurrence.active, onCheckedChange = { onToggle() })
                }
                TextButton(onClick = onDelete) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun UpcomingOccurrences(
    occurrences: List<com.example.lev.domain.RecurrenceOccurrence>,
    onConfirm: (com.example.lev.domain.RecurrenceOccurrence) -> Unit,
) {
    if (occurrences.isEmpty()) {
        Text(
            text = "Nenhuma ocorrência futura pendente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Text("Próximas ocorrências", style = MaterialTheme.typography.labelLarge)
    occurrences.forEach { occurrence ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = occurrence.date.toString(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                occurrence.amountCents?.let { amount ->
                    MoneyText(
                        cents = amount,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = false,
                    )
                }
                TextButton(onClick = { onConfirm(occurrence) }) {
                    Text("Lançar")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateRecurrenceDialog(
    accounts: List<com.example.lev.data.model.Account>,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        description: String,
        amountCents: Long,
        isVariable: Boolean,
        accountId: String,
        categoryId: String?,
        frequency: String,
        interval: Int,
        startDate: java.time.LocalDate,
    ) -> Unit,
) {
    var type by remember { mutableStateOf("EXPENSE") }
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isVariable by remember { mutableStateOf(false) }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var frequency by remember { mutableStateOf("MONTHLY") }
    var intervalText by remember { mutableStateOf("1") }
    var startDateText by remember { mutableStateOf(java.time.LocalDate.now().toString()) }
    var startDateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova recorrência") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == "EXPENSE",
                        onClick = { type = "EXPENSE" },
                        label = { Text("Despesa") },
                    )
                    FilterChip(
                        selected = type == "INCOME",
                        onClick = { type = "INCOME" },
                        label = { Text("Receita") },
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
                    onValueChange = { amountText = it },
                    label = { Text("Valor (R$)") },
                    enabled = !isVariable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Valor variável", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isVariable, onCheckedChange = { isVariable = it })
                }

                Text("Conta", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = account.id == selectedAccountId,
                            onClick = { selectedAccountId = account.id },
                            label = { Text(account.name) },
                        )
                    }
                }

                Text("Frequência", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "DAILY" to "Diária",
                        "WEEKLY" to "Semanal",
                        "MONTHLY" to "Mensal",
                        "YEARLY" to "Anual",
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = frequency == value,
                            onClick = { frequency = value },
                            label = { Text(label) },
                        )
                    }
                }

                OutlinedTextField(
                    value = intervalText,
                    onValueChange = { intervalText = it.filter { c -> c.isDigit() } },
                    label = { Text("A cada X períodos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = startDateText,
                    onValueChange = {
                        startDateText = it
                        startDateError = false
                    },
                    label = { Text("Data inicial (AAAA-MM-DD)") },
                    isError = startDateError,
                    supportingText = if (startDateError) ({ Text("Use o formato AAAA-MM-DD") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled =
                    description.isNotBlank() &&
                        selectedAccountId.isNotBlank() &&
                        (isVariable || MoneyParser.parse(amountText) > 0L) &&
                        !startDateError,
                onClick = {
                    val startDate =
                        RecurrencesViewModel.parseDate(startDateText.trim()) ?: run {
                            startDateError = true
                            return@Button
                        }
                    onSave(
                        type,
                        description.trim(),
                        MoneyParser.parse(amountText),
                        isVariable,
                        selectedAccountId,
                        selectedCategoryId,
                        frequency,
                        intervalText.toIntOrNull() ?: 1,
                        startDate,
                    )
                },
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
