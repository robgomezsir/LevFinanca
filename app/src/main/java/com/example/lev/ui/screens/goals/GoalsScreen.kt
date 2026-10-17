package com.example.lev.ui.screens.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.domain.MoneyFormatter
import com.example.lev.domain.MoneyParser
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LevProgressBar
import com.example.lev.ui.components.LoadingList
import com.example.lev.ui.components.MoneyText

@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<GoalListItem?>(null) }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Metas",
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(
                onClick = { showCreate = true },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Nova meta")
            }
        }

        if (uiState.isLoading) {
            LoadingList()
        } else if (uiState.goals.isEmpty()) {
            EmptyState(
                title = "Nenhuma meta criada",
                description =
                    "Crie metas para juntar dinheiro com propósito: " +
                        "viagem, reserva de emergência, carro novo.",
                actionLabel = "Criar meta",
                onAction = { showCreate = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(uiState.goals, key = { it.goal.id }) { item ->
                    GoalCard(
                        item = item,
                        onContribute = { contributingGoal = item },
                        onDelete = { viewModel.deleteGoal(item.goal.id) },
                    )
                }
            }
        }
    }

    if (showCreate) {
        CreateGoalDialog(
            onDismiss = { showCreate = false },
            onSave = { name, cents, date ->
                viewModel.createGoal(name, cents, date)
                showCreate = false
            },
        )
    }

    contributingGoal?.let { item ->
        ContributionDialog(
            goalName = item.goal.name,
            onDismiss = { contributingGoal = null },
            onSave = { cents ->
                viewModel.addContribution(item.goal.id, cents)
                contributingGoal = null
            },
        )
    }
}

@Composable
private fun GoalCard(
    item: GoalListItem,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
) {
    val progress = item.progress

    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(16.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "Meta ${item.goal.name}: ${progress.percent} por cento, " +
                            "juntado ${MoneyFormatter.format(progress.savedCents)} de " +
                            MoneyFormatter.format(progress.targetCents)
                    },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (progress.isAchieved) {
                    Text(
                        text = "Meta atingida",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            LevProgressBar(
                fraction = progress.percent / 100f,
                color =
                    if (progress.isAchieved) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                label = "${progress.percent}% juntado",
                contentDescription = "Meta ${progress.percent} por cento juntada",
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Juntado", style = MaterialTheme.typography.labelSmall)
                    MoneyText(
                        cents = progress.savedCents,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = false,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Falta", style = MaterialTheme.typography.labelSmall)
                    MoneyText(
                        cents = progress.remainingCents,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = true,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Alvo: ${MoneyFormatter.format(progress.targetCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (progress.dailySuggestedCents > 0L) {
                    Text(
                        text = "Sugestão: ${MoneyFormatter.format(progress.dailySuggestedCents)}/dia",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onContribute, modifier = Modifier.weight(1f)) {
                    Text("Aportar")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CreateGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, Long, java.time.LocalDate?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova meta") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Valor alvo (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        dateError = false
                    },
                    label = { Text("Data alvo (AAAA-MM-DD, opcional)") },
                    isError = dateError,
                    supportingText = if (dateError) ({ Text("Use o formato AAAA-MM-DD") }) else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && MoneyParser.parse(targetText) > 0L && !dateError,
                onClick = {
                    val date =
                        if (dateText.isBlank()) {
                            null
                        } else {
                            try {
                                java.time.LocalDate.parse(dateText.trim())
                            } catch (_: java.time.format.DateTimeParseException) {
                                dateError = true
                                return@TextButton
                            }
                        }
                    onSave(name.trim(), MoneyParser.parse(targetText), date)
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

@Composable
private fun ContributionDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aportar em \"$goalName\"") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Valor do aporte (R$)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                enabled = MoneyParser.parse(amountText) > 0L,
                onClick = { onSave(MoneyParser.parse(amountText)) },
            ) {
                Text("Aportar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
