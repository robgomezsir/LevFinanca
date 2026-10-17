package com.example.lev.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.FilterChip
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
import com.example.lev.domain.BudgetSnapshot
import com.example.lev.domain.MoneyFormatter
import com.example.lev.domain.MoneyParser
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LevProgressBar
import com.example.lev.ui.components.LoadingList
import com.example.lev.ui.components.MoneyText

@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editingCategory by remember { mutableStateOf<BudgetSnapshot?>(null) }
    var creatingNew by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        LoadingList(modifier = modifier)
        return
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Orçamento de ${uiState.month.monthValue.toString().padStart(2, '0')}/${uiState.month.year}",
                style = MaterialTheme.typography.headlineSmall,
            )
            TextButton(
                onClick = { creatingNew = true },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text("Definir")
            }
        }

        if (uiState.snapshots.isEmpty()) {
            EmptyState(
                title = "Nenhum orçamento definido",
                description = "Defina um valor planejado por categoria para acompanhar seus gastos do mês.",
                actionLabel = "Criar orçamento",
                onAction = { creatingNew = true },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(uiState.snapshots, key = { it.categoryId }) { snapshot ->
                    BudgetCard(snapshot = snapshot, onClick = { editingCategory = snapshot })
                }
            }
        }
    }

    editingCategory?.let { snapshot ->
        EditBudgetDialog(
            snapshot = snapshot,
            onDismiss = { editingCategory = null },
            onSave = { cents ->
                viewModel.savePlannedAmount(snapshot.categoryId, cents)
                editingCategory = null
            },
        )
    }

    if (creatingNew) {
        CreateBudgetDialog(
            categories = uiState.expenseCategories.filter { cat -> uiState.snapshots.none { it.categoryId == cat.id } },
            onDismiss = { creatingNew = false },
            onSave = { categoryId, cents ->
                viewModel.savePlannedAmount(categoryId, cents)
                creatingNew = false
            },
        )
    }
}

@Composable
private fun BudgetCard(
    snapshot: BudgetSnapshot,
    onClick: () -> Unit,
) {
    LevCard(
        onClick = onClick,
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
                            "Orçamento ${snapshot.categoryName}: ${snapshot.percentUsed} por cento usado, " +
                            "gasto ${MoneyFormatter.format(snapshot.spentCents)} de " +
                            MoneyFormatter.format(snapshot.plannedCents)
                    },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = snapshot.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (snapshot.isOverBudget) {
                    Text(
                        text = "Acima do orçamento",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            BudgetProgressBar(percentUsed = snapshot.percentUsed, isOverBudget = snapshot.isOverBudget)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Gasto", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        cents = -snapshot.spentCents,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = true,
                        showSign = true,
                    )
                }
                Column {
                    Text("Planejado", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        cents = snapshot.plannedCents,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = false,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (snapshot.isOverBudget) "Excedente" else "Restante",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    MoneyText(
                        cents = snapshot.remainingCents,
                        style = MaterialTheme.typography.bodyMedium,
                        useSemanticColor = true,
                        showSign = true,
                    )
                }
            }

            if (snapshot.rolloverCents != 0L) {
                Text(
                    text = "Rollover do mês anterior: ${MoneyFormatter.format(snapshot.rolloverCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(
    percentUsed: Int,
    isOverBudget: Boolean,
) {
    val color =
        when {
            isOverBudget -> MaterialTheme.colorScheme.error
            percentUsed >= 80 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }

    LevProgressBar(
        fraction = percentUsed / 100f,
        color = color,
        label = "$percentUsed% usado",
        contentDescription = "Orçamento $percentUsed por cento usado",
    )
}

@Composable
private fun EditBudgetDialog(
    snapshot: BudgetSnapshot,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
) {
    var amountText by remember { mutableStateOf(MoneyFormatter.formatNumberOnly(snapshot.plannedCents)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Orçamento — ${snapshot.categoryName}") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor planejado (R$)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text =
                        "Gasto neste mês: ${MoneyFormatter.format(snapshot.spentCents)}. " +
                            "Informe 0 para remover o orçamento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(MoneyParser.parse(amountText)) }) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateBudgetDialog(
    categories: List<com.example.lev.data.model.Category>,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(categories.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo orçamento") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (categories.isEmpty()) {
                    Text(
                        text = "Todas as categorias de despesa já têm orçamento neste mês.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text("Categoria", style = MaterialTheme.typography.labelLarge)
                    FlowRow {
                        categories.forEach { category ->
                            FilterChip(
                                selected = category.id == selectedCategoryId,
                                onClick = { selectedCategoryId = category.id },
                                label = { Text(category.name) },
                                modifier = Modifier.padding(end = 4.dp, bottom = 4.dp),
                            )
                        }
                    }
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Valor planejado (R$)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedCategoryId != null && MoneyParser.parse(amountText) > 0L,
                onClick = {
                    val categoryId = selectedCategoryId ?: return@TextButton
                    onSave(categoryId, MoneyParser.parse(amountText))
                },
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
