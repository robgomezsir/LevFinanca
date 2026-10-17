package com.example.lev.ui.screens.reports

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.domain.Insight
import com.example.lev.domain.MoneyFormatter
import com.example.lev.domain.NetWorthSnapshot
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LevProgressBar
import com.example.lev.ui.components.LevSectionHeader
import com.example.lev.ui.components.LoadingList
import com.example.lev.ui.components.MoneyText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    modifier: Modifier = Modifier,
    onOpenPlanning: () -> Unit = {},
    onOpenNetWorth: () -> Unit = {},
    onOpenInsightTarget: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Relatórios", style = MaterialTheme.typography.headlineSmall) })

        if (uiState.isLoading) {
            LoadingList()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    NetWorthCard(uiState.netWorth, onClick = onOpenNetWorth)
                }

                // Orçamento do mês
                if (uiState.budgetSnapshots.isNotEmpty()) {
                    item {
                        LevSectionHeader(
                            title = "Orçamento do mês",
                            actionLabel = "Gerenciar",
                            onAction = onOpenPlanning,
                        )
                    }
                    items(
                        uiState.budgetSnapshots.filter { it.plannedCents > 0L },
                        key = { it.categoryId },
                    ) { snapshot ->
                        BudgetSummaryRow(snapshot, onClick = onOpenPlanning)
                    }
                }

                // Insights
                if (uiState.insights.isNotEmpty()) {
                    item {
                        LevSectionHeader(title = "Insights")
                    }
                    items(uiState.insights, key = { it.title }) { insight ->
                        InsightCard(insight, onClick = { onOpenInsightTarget(insight.sourceScreen) })
                    }
                }

                // Empty state
                if (uiState.netWorth == null && uiState.insights.isEmpty()) {
                    item {
                        EmptyState(
                            title = "Sem relatórios ainda",
                            description = "Adicione lançamentos para ver patrimônio, orçamento e insights.",
                            actionLabel = "Gerenciar orçamento",
                            onAction = onOpenPlanning,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthCard(
    networth: NetWorthSnapshot?,
    onClick: (() -> Unit)? = null,
) {
    if (networth == null) return

    LevCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(20.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            "Patrimônio líquido ${MoneyFormatter.format(networth.netWorthCents)}, " +
                            "ativos ${MoneyFormatter.format(networth.assetsCents)}, " +
                            "passivos ${MoneyFormatter.format(networth.liabilitiesCents)}"
                    },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Patrimônio líquido",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Ativos", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        cents = networth.assetsCents,
                        style = MaterialTheme.typography.bodyLarge,
                        useSemanticColor = false,
                    )
                }
                Column {
                    Text("Passivos", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        cents = networth.liabilitiesCents,
                        style = MaterialTheme.typography.bodyLarge,
                        useSemanticColor = false,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Líquido", style = MaterialTheme.typography.labelMedium)
                    MoneyText(
                        cents = networth.netWorthCents,
                        style = MaterialTheme.typography.titleMedium,
                        useSemanticColor = false,
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetSummaryRow(
    snapshot: com.example.lev.domain.BudgetSnapshot,
    onClick: () -> Unit = {},
) {
    LevCard(
        onClick = onClick,
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
                Text(
                    snapshot.categoryName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "${snapshot.percentUsed}%",
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        if (snapshot.isOverBudget) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                )
            }
            LevProgressBar(
                fraction = snapshot.percentUsed / 100f,
                contentDescription =
                    "${snapshot.categoryName}: ${snapshot.percentUsed} por cento usado, " +
                        "gasto ${MoneyFormatter.format(snapshot.spentCents)} de " +
                        MoneyFormatter.format(snapshot.plannedCents + snapshot.rolloverCents),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MoneyText(
                    cents = -snapshot.spentCents,
                    style = MaterialTheme.typography.bodyMedium,
                    useSemanticColor = true,
                    showSign = true,
                )
                MoneyText(
                    cents = snapshot.plannedCents + snapshot.rolloverCents,
                    style = MaterialTheme.typography.bodyMedium,
                    useSemanticColor = false,
                )
            }
        }
    }
}

@Composable
fun InsightCard(
    insight: Insight,
    onClick: () -> Unit = {},
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
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
