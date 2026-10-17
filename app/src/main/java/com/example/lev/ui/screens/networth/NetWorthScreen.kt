package com.example.lev.ui.screens.networth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lev.ui.AppViewModelProvider
import com.example.lev.ui.screens.reports.NetWorthCard
import com.example.lev.ui.screens.reports.ReportsViewModel
import com.example.lev.ui.theme.PoiretOne

/** Patrimônio líquido (M6): tela própria; Relatórios mantém o resumo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val reportsViewModel: ReportsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val uiState by reportsViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Patrimônio", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { NetWorthCard(uiState.netWorth) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Ativos menos passivos",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Contas e investimentos entram como ativos; cartões e empréstimos " +
                            "como passivos. O snapshot mensal e o gráfico de evolução entram no M6.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
