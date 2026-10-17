package com.example.lev.ui.screens.sync

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.data.repository.SyncChipState
import com.example.lev.data.repository.SyncMode
import com.example.lev.ui.components.SyncStatusChip
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.theme.LevTheme
import com.example.lev.ui.theme.PoiretOne
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Configurações > Planilha (M7 §F): URL, segredo, testar, enviar, baixar,
 * modo Mesclar/Substituir, status, logs e o script com passo a passo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncSettingsScreen(
    viewModel: SyncViewModel,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    val syncLog by viewModel.syncLog.collectAsStateWithLifecycle()
    val conflicts by viewModel.conflicts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(action.message) {
        action.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planilha", fontFamily = PoiretOne) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                StatusCard(
                    chip = uiState.chip,
                    pendingCount = uiState.pendingCount,
                    lastMessage = uiState.lastMessage,
                    lastSyncAt = uiState.lastSyncAt,
                )
            }
            item {
                ConnectionCard(
                    hasSecret = uiState.hasSecret,
                    savedUrlHost = uiState.savedUrlHost,
                    busy = action.busy,
                    onSave = viewModel::saveConnection,
                    onTest = viewModel::testConnection,
                )
            }
            item {
                ModeCard(
                    mode = uiState.mode,
                    autoSync = uiState.autoSync,
                    busy = action.busy,
                    configured = uiState.hasUrl && uiState.hasSecret,
                    onMode = viewModel::setMode,
                    onAuto = viewModel::setAutoSync,
                    onSyncNow = viewModel::syncNow,
                    onPushReplace = viewModel::askPushReplace,
                    onPullReplace = viewModel::askPullReplace,
                    onCompact = viewModel::compact,
                )
            }
            item {
                ScriptCard(scriptText = viewModel.scriptText())
            }
            if (conflicts.isNotEmpty()) {
                item {
                    Text(
                        "Conflitos (${conflicts.size})",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(conflicts, key = { it.tab + it.id }) { conflict ->
                    Text(
                        "• ${conflict.tab} ${conflict.id}: ${conflict.detail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (syncLog.isNotEmpty()) {
                item {
                    Text("Registro de sincronização", style = MaterialTheme.typography.titleMedium)
                }
                items(syncLog.take(20), key = { it.at }) { entry ->
                    val time = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(entry.at))
                    Text(
                        "$time — ${entry.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    action.pendingConfirm?.let { kind ->
        ReplaceConfirmDialog(
            kind = kind,
            counts = action.confirmCounts,
            onConfirm = viewModel::confirmReplace,
            onDismiss = viewModel::dismissConfirm,
        )
    }
}

@Composable
private fun StatusCard(
    chip: SyncChipState,
    pendingCount: Int,
    lastMessage: String,
    lastSyncAt: Long?,
) {
    LevCard(
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
                Text("Estado", style = MaterialTheme.typography.titleSmall)
                SyncStatusChip(chip = chip, pendingCount = pendingCount)
            }
            if (lastSyncAt != null) {
                val time =
                    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(lastSyncAt))
                Text(
                    "Último sync: $time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(lastMessage, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ConnectionCard(
    hasSecret: Boolean,
    savedUrlHost: String,
    busy: Boolean,
    onSave: (String, String) -> Unit,
    onTest: () -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    var secret by rememberSaveable { mutableStateOf("") }
    var secretVisible by rememberSaveable { mutableStateOf(false) }

    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Conexão", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL /exec da implantação") },
                placeholder = { Text("https://script.google.com/macros/s/…/exec") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = secret,
                onValueChange = { secret = it },
                label = { Text(if (hasSecret) "Segredo (salvo ••••, preencha para trocar)" else "Segredo") },
                singleLine = true,
                visualTransformation =
                    if (secretVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onSave(url, secret) },
                    enabled = !busy && url.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Salvar conexão")
                }
                Button(
                    onClick = onTest,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Testar conexão")
                }
            }
            Text(
                "O segredo fica cifrado no aparelho (Keystore) e nunca sai dele em texto puro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (savedUrlHost.isNotBlank()) {
                Text(
                    "URL salva: $savedUrlHost",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    mode: SyncMode,
    autoSync: Boolean,
    busy: Boolean,
    configured: Boolean,
    onMode: (SyncMode) -> Unit,
    onAuto: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onPushReplace: () -> Unit,
    onPullReplace: () -> Unit,
    onCompact: () -> Unit,
) {
    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Modo e operações", style = MaterialTheme.typography.titleSmall)
            Column(modifier = Modifier.selectableGroup()) {
                ModeOption(
                    selected = mode == SyncMode.MERGE,
                    title = "Mesclar (padrão)",
                    description = "Junta app e planilha pelo mais recente, sem perder nada.",
                    onClick = { onMode(SyncMode.MERGE) },
                )
                ModeOption(
                    selected = mode == SyncMode.REPLACE,
                    title = "Substituir",
                    description = "Enviar sobrescreve as abas; Baixar substitui o banco. Pede confirmação e backup.",
                    onClick = { onMode(SyncMode.REPLACE) },
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Sincronização automática", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Envia cada novidade em segundos, sem toque manual. " +
                            "O merge completo continua no botão acima.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = autoSync, onCheckedChange = onAuto)
            }
            Button(onClick = onSyncNow, enabled = !busy && configured, modifier = Modifier.fillMaxWidth()) {
                Text("Sincronizar agora")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onPushReplace,
                    enabled = !busy && configured,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Enviar tudo")
                }
                Button(
                    onClick = onPullReplace,
                    enabled = !busy && configured,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Baixar tudo")
                }
            }
            TextButton(onClick = onCompact, enabled = !busy && configured) {
                Text("Compactar lixeira antiga (> 90 dias)")
            }
        }
    }
}

@Composable
private fun ModeOption(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScriptCard(scriptText: String) {
    val clipboard = LocalClipboardManager.current
    var expanded by rememberSaveable { mutableStateOf(false) }

    LevCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Script da planilha", style = MaterialTheme.typography.titleSmall)
            Text(
                "1. Crie uma planilha no Google Drive.\n" +
                    "2. Use o script DENTRO dela (Extensões > Apps Script) ou um projeto " +
                    "avulso — no avulso, preencha PLANILHA_ID com o ID da URL da planilha.\n" +
                    "3. Segredo SEM rodar função: engrenagem (Configurações do projeto) > " +
                    "Propriedades do script > adicione LEV_SECRET. Alternativa: preencha " +
                    "SEGREDO_DO_APP e rode configurarSegredo(). Use o MESMO segredo no app.\n" +
                    "4. Em Configurações do projeto, mostre o appsscript.json e confira os " +
                    "escopos; Implantar > Nova implantação > Aplicativo da Web: Executar " +
                    "como \"Eu\", acesso \"Qualquer pessoa\" (autorize com a sua conta).\n" +
                    "5. Copie a URL /exec para o campo de conexão acima.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { clipboard.setText(AnnotatedString(scriptText)) },
                    enabled = scriptText.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Copiar código")
                }
                FilterChip(
                    selected = expanded,
                    onClick = { expanded = !expanded },
                    label = { Text(if (expanded) "Ocultar" else "Ver código") },
                )
            }
            if (expanded && scriptText.isNotBlank()) {
                SelectionContainer {
                    Text(
                        text = scriptText,
                        style = MaterialTheme.typography.bodySmall,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReplaceConfirmDialog(
    kind: ConfirmKind,
    counts: Map<String, Int>,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isPush = kind == ConfirmKind.PUSH_REPLACE
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isPush) "Enviar tudo para a planilha?" else "Substituir o banco pela planilha?") },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    if (isPush) {
                        "As abas de dados serão SOBRESCRITAS com o conteúdo do app."
                    } else {
                        "O banco local será SUBSTITUÍDO pelo conteúdo da planilha."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Backup automático antes: JSON no aparelho + abas _bkp na planilha.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                counts.entries.sortedBy { it.key }.forEach { (tab, count) ->
                    Text("$tab: $count", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Confirmar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SyncStatusChipsPreview() {
    LevTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SyncStatusChip(chip = SyncChipState.SYNCED)
            SyncStatusChip(chip = SyncChipState.PENDING, pendingCount = 3)
            SyncStatusChip(chip = SyncChipState.OFFLINE)
            SyncStatusChip(chip = SyncChipState.ERROR)
            SyncStatusChip(chip = SyncChipState.SYNCING)
            SyncStatusChip(chip = SyncChipState.NEVER)
        }
    }
}
