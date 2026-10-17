package com.example.lev.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.model.Account
import com.example.lev.ui.components.AnimatedMoneyText
import com.example.lev.ui.components.BrandGradientCard
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LevSectionHeader
import com.example.lev.ui.components.MoneyText
import com.example.lev.ui.theme.OnBrandGradient

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCardClick: (Account) -> Unit = {},
    onEmptyAction: () -> Unit = {},
    onOpenAccounts: () -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
    onBalanceClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.cards.isEmpty()) {
        EmptyState(
            title = "Nenhuma conta cadastrada",
            description = "Cadastre uma conta para começar a controlar suas finanças.",
            modifier = Modifier.fillMaxSize(),
            actionLabel = "Começar",
            onAction = onEmptyAction,
        )
        return
    }

    val creditCards = uiState.cards.filter { it.account.type == "CREDIT_CARD" }
    val otherAccounts = uiState.cards.filter { it.account.type != "CREDIT_CARD" }

    val totalBalanceCents =
        otherAccounts
            .filter { it.account.includeInTotals }
            .sumOf { it.currentBalanceCents }
    val totalCardDebtCents = creditCards.sumOf { it.currentInvoiceTotalCents }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            BalanceSummaryCard(
                totalBalanceCents = totalBalanceCents,
                totalCardDebtCents = totalCardDebtCents,
                onClick = onBalanceClick,
            )
        }

        if (creditCards.isNotEmpty()) {
            item {
                LevSectionHeader(title = "Cartões de crédito")
            }
            items(creditCards) { card ->
                CreditCardItem(card, onCardClick)
            }
        }

        if (otherAccounts.isNotEmpty()) {
            item {
                LevSectionHeader(
                    title = "Outras contas",
                    actionLabel = "Ver contas",
                    onAction = onOpenAccounts,
                )
            }
            items(otherAccounts) { card ->
                AccountItem(card, onAccountClick)
            }
        }
    }
}

/**
 * Card de saldo — uma das duas superfícies com o gradiente da marca (a outra é o topo).
 * Exposto ao design system: `LevDesignScreenshotsTest` renderiza e congela este card.
 */
@Composable
internal fun BalanceSummaryCard(
    totalBalanceCents: Long,
    totalCardDebtCents: Long,
    onClick: () -> Unit,
) {
    // Segunda superfície do gradiente da marca (a primeira é o topo do app): o conteúdo
    // herda branco puro. Sem cor semântica aqui — vermelho/verde não passam em AA sobre
    // o ciano — o sinal "−/+" é quem carrega a semântica (Base §7).
    BrandGradientCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Saldo total",
                style = MaterialTheme.typography.labelLarge,
            )
            AnimatedMoneyText(
                cents = totalBalanceCents,
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Traço do livro-caixa em branco: o GradientRule repetiria o próprio gradiente
            // do card e ficaria camuflado.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(OnBrandGradient.copy(alpha = 0.4f)),
            )
            if (totalCardDebtCents > 0L) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Faturas em aberto",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MoneyText(
                        cents = -totalCardDebtCents,
                        style = MaterialTheme.typography.bodyLarge,
                        useSemanticColor = false,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditCardItem(
    card: CardSummaryItem,
    onCardClick: (Account) -> Unit,
) {
    LevCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCardClick(card.account) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = card.account.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AccountTypeBadge(label = "Cartão")
            }

            LedgerAmountRow(label = "Limite", cents = card.account.creditLimitCents ?: 0L, signed = false)
            LedgerAmountRow(label = "Usado", cents = -card.usedCents, signed = false)
            LedgerAmountRow(label = "Disponível", cents = card.availableCents, signed = false)

            if (card.futureInstallmentsCents > 0L) {
                LedgerAmountRow(label = "Parcelas futuras", cents = card.futureInstallmentsCents, signed = false)
            }

            if (card.invoiceHistory.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Histórico de faturas",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    card.invoiceHistory.take(3).forEach { invoice ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "${invoice.monthLabel} · vence ${invoice.dueDateLabel}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            MoneyText(cents = invoice.totalCents, useSemanticColor = false)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Fatura aberta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(
                    cents = card.currentInvoiceTotalCents,
                    useSemanticColor = false,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

@Composable
private fun LedgerAmountRow(
    label: String,
    cents: Long,
    signed: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        MoneyText(cents = cents, useSemanticColor = signed, showSign = signed)
    }
}

@Composable
private fun AccountItem(
    card: CardSummaryItem,
    onAccountClick: (Account) -> Unit,
) {
    val account = card.account
    val accountTypeLabel =
        when (account.type) {
            "CHECKING" -> "Conta corrente"
            "SAVINGS" -> "Poupança"
            "CASH" -> "Dinheiro"
            "INVESTMENT" -> "Investimento"
            "BENEFIT" -> "Benefício"
            "LOAN" -> "Empréstimo"
            "CREDIT_CARD" -> "Cartão"
            else -> account.type
        }
    LevCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onAccountClick(account) },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                AccountTypeBadge(label = accountTypeLabel)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Saldo")
                MoneyText(cents = card.currentBalanceCents, useSemanticColor = false)
            }
        }
    }
}

/** Badge do tipo de conta: par primário/primárioContainer medido (5.69:1 claro, 6.52:1 escuro). */
@Composable
private fun AccountTypeBadge(label: String) {
    Box(
        modifier =
            Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
