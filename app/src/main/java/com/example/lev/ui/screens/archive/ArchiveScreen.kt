package com.example.lev.ui.screens.archive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.theme.PoiretOne

/** Arquivo de meses fechados (M5). Lista real entra com MonthClosure + reabrir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arquivo", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        EmptyState(
            title = "Nenhum mês fechado",
            description =
                "Ao fechar um mês (receitas, despesas e saldo), o resumo aparece aqui " +
                    "com opção Reabrir. Fechamento entra no M5.",
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}
