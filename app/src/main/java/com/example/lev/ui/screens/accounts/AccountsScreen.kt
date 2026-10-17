package com.example.lev.ui.screens.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.model.Account
import com.example.lev.domain.MoneyFormatter
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.MoneyText
import com.example.lev.ui.theme.PoiretOne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contas", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar conta")
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.accounts.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = "Nenhuma conta cadastrada",
                    description = "Toque no + para criar uma conta.",
                )
            }
        } else {
            AccountList(
                accounts = uiState.accounts,
                balances = uiState.balances,
                onAccountClick = { onAccountClick(it) },
                onDeleteClick = { viewModel.deleteAccount(id = it) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    if (showCreateDialog) {
        CreateAccountDialog(
            viewModel = viewModel,
            onDismiss = { showCreateDialog = false },
        )
    }
}

@Composable
fun AccountList(
    accounts: List<Account>,
    balances: Map<String, Long>,
    onAccountClick: (Account) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(accounts, key = { it.id }) { account ->
            AccountItem(
                account = account,
                balanceCents = balances[account.id] ?: account.initialBalanceCents,
                onClick = { onAccountClick(account) },
                onDeleteClick = { onDeleteClick(account.id) },
            )
        }
    }
}

@Composable
fun AccountItem(
    account: Account,
    balanceCents: Long,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    val typeLabel =
        mapOf(
            "CHECKING" to "Conta corrente",
            "SAVINGS" to "Poupança",
            "CASH" to "Dinheiro",
            "CREDIT_CARD" to "Cartão de crédito",
            "INVESTMENT" to "Investimento",
            "BENEFIT" to "Benefício",
            "LOAN" to "Empréstimo",
        )[account.type] ?: account.type

    LevCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        cents = balanceCents,
                        style = MaterialTheme.typography.titleMedium,
                        useSemanticColor = false,
                    )
                    if (account.type == "CREDIT_CARD") {
                        Text(
                            text = "Fatura em aberto",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = "Saldo atual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    account.creditLimitCents?.let { limitCents ->
                        Text(
                            // Bug documentado no audit: formatava com template
                            // cru (R$ 5000.0). Usa o MoneyFormatter do domínio.
                            text = "Limite: ${MoneyFormatter.format(limitCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
fun CreateAccountDialog(
    viewModel: AccountsViewModel,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf("CHECKING") }
    var initialBalance by rememberSaveable { mutableStateOf("") }
    var creditLimit by rememberSaveable { mutableStateOf("") }

    val isCreditCard = selectedType == "CREDIT_CARD"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta") },
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
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    viewModel.accountTypes.forEach { type ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedType = type },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                            )
                            Text(
                                text = viewModel.accountTypeLabels[type] ?: type,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { initialBalance = it },
                    label = { Text("Saldo inicial (R$)") },
                    singleLine = true,
                    keyboardOptions =
                        KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (isCreditCard) {
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = { Text("Limite do cartão (R$)") },
                        singleLine = true,
                        keyboardOptions =
                            KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val balanceCents =
                        (initialBalance.toDoubleOrNull() ?: 0.0).times(100).toLong()
                    val limitCents =
                        if (isCreditCard) {
                            (creditLimit.toDoubleOrNull() ?: 0.0).times(100).toLong()
                        } else {
                            null
                        }
                    if (name.isNotBlank()) {
                        viewModel.createAccount(
                            name = name,
                            type = selectedType,
                            initialBalanceCents = balanceCents,
                            creditLimitCents = limitCents,
                        )
                    }
                    onDismiss()
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
