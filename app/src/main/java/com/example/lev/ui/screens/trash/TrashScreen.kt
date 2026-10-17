package com.example.lev.ui.screens.trash

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

/** Lixeira de 30 dias (M5): restaura soft delete e purga definitiva. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lixeira", fontFamily = PoiretOne) },
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
            title = "Lixeira vazia",
            description = "Itens excluídos ficam aqui por 30 dias com opção Restaurar antes da purga definitiva.",
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
    }
}
