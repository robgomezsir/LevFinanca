package com.example.lev.ui.screens.importexport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.theme.PoiretOne

/** Importar / Exportar (M8): mapeamento CSV, backup JSON e PDF entram aqui. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar / Exportar", fontFamily = PoiretOne) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ImportExportCard(
                    title = "Importar CSV",
                    description =
                        "Mapeamento de colunas, perfis de bancos BR, pré-visualização e " +
                            "desfazer lote (importBatchId).",
                    actionLabel = "Em breve (M8)",
                    onAction = {},
                )
            }
            item {
                ImportExportCard(
                    title = "Exportar CSV / JSON",
                    description = "Exporte por período ou faça backup completo versionado para restaurar depois.",
                    actionLabel = "Em breve (M8)",
                    onAction = {},
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "A planilha não substitui o backup local",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Mantenha exportações periódicas em local seguro. OFX fica para a v1.1.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportExportCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAction, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}
