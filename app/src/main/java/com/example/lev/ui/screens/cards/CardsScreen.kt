package com.example.lev.ui.screens.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.MoneyText
import com.example.lev.ui.theme.PoiretOne

/** Lista de cartões (M3): cada cartão abre a fatura correspondente. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    financeRepository: FinanceRepository,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onOpenInvoice: (String) -> Unit = {},
    onCreateCard: () -> Unit = {},
) {
    val accounts by financeRepository.getAccounts().collectAsStateWithLifecycle(initialValue = emptyList())
    val cards = accounts.filter { it.type == "CREDIT_CARD" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cartões", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (cards.isEmpty()) {
            EmptyState(
                title = "Nenhum cartão cadastrado",
                description = "Cadastre um cartão em Contas com tipo Cartão de crédito para ver limite e faturas aqui.",
                actionLabel = "Ir para contas",
                onAction = onCreateCard,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(cards, key = { it.id }) { card ->
                    LevCard(
                        onClick = { onOpenInvoice(card.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(card.name, style = MaterialTheme.typography.titleMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Limite", style = MaterialTheme.typography.bodyMedium)
                                MoneyText(cents = card.creditLimitCents ?: 0L, useSemanticColor = false)
                            }
                            Text(
                                "Fechamento dia ${card.closingDay ?: 25} • Vencimento dia ${card.dueDay ?: 5}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
