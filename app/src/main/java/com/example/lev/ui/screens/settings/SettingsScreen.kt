package com.example.lev.ui.screens.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lev.ui.components.SettingItem
import com.example.lev.ui.components.SettingsSection
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.PoiretOne
import com.example.lev.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    onOpenCategories: () -> Unit = {},
    onOpenSheets: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("Configurações", fontFamily = PoiretOne) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                    )
                }
            },
        )

        SettingsSection(title = "Organização") {
            SettingItem(
                icon = Icons.Default.Category,
                title = "Categorias",
                description = "Editar categorias e subcategorias",
                onClick = onOpenCategories,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }

        SettingsSection(title = "Aparência") {
            AppearanceSection(
                themeMode = uiState.themeMode,
                appearance = uiState.appearance,
                dynamicColor = uiState.dynamicColor,
                onThemeMode = viewModel::setThemeMode,
                onAppearance = viewModel::setAppearance,
                onDynamicColor = viewModel::setDynamicColor,
            )
        }

        SettingsSection(title = "Privacidade") {
            SettingItem(
                icon = Icons.Default.PrivacyTip,
                title = "Biometria",
                description = "Usar biometria para acessar o app",
                trailing = {
                    Switch(
                        checked = uiState.useBiometrics,
                        onCheckedChange = { viewModel.setUseBiometrics(it) },
                    )
                },
            )
            SettingItem(
                icon = Icons.Default.Notifications,
                title = "Lembretes de contas",
                description = "Notificar sobre contas próximas ao vencimento",
                trailing = {
                    Switch(
                        checked = uiState.accountRemindersEnabled,
                        onCheckedChange = { viewModel.setAccountReminders(it) },
                    )
                },
            )
        }

        SettingsSection(title = "Sincronização") {
            SettingItem(
                icon = Icons.Default.Sync,
                title = "Planilha Google",
                description =
                    when {
                        uiState.syncSheetsConfigured -> "Sincronizar via Apps Script"
                        else -> "Conectar planilha via Apps Script"
                    },
                onClick = onOpenSheets,
                trailing = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            SettingItem(
                icon = Icons.Default.Sync,
                title = "Sincronizar com a nuvem",
                description = if (uiState.syncEnabled) "Ativo" else "Desativado",
                trailing = {
                    Switch(
                        checked = uiState.syncEnabled,
                        onCheckedChange = { viewModel.setSyncEnabled(it) },
                    )
                },
            )
            if (uiState.syncEnabled) {
                var showServerDialog by rememberSaveable { mutableStateOf(false) }
                SettingItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Servidor",
                    description = uiState.syncServer.ifEmpty { "Não configurado" },
                    trailing = {
                        TextButton(onClick = { showServerDialog = true }) {
                            Text("Alterar")
                        }
                    },
                )
                if (showServerDialog) {
                    var serverText by rememberSaveable { mutableStateOf(uiState.syncServer) }
                    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
                    AlertDialog(
                        onDismissRequest = { showServerDialog = false },
                        title = { Text("Configurar Servidor") },
                        text = {
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                OutlinedTextField(
                                    value = serverText,
                                    onValueChange = { serverText = it },
                                    label = { Text("URL do servidor") },
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = uiState.syncToken,
                                    onValueChange = { viewModel.setSyncToken(it) },
                                    label = { Text("Token de acesso") },
                                    singleLine = true,
                                    visualTransformation =
                                        if (isPasswordVisible) {
                                            VisualTransformation.None
                                        } else {
                                            PasswordVisualTransformation()
                                        },
                                    trailingIcon = {
                                        val icon =
                                            if (isPasswordVisible) {
                                                Icons.Default.VisibilityOff
                                            } else {
                                                Icons.Default.Visibility
                                            }
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(imageVector = icon, contentDescription = null)
                                        }
                                    },
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.setSyncServer(serverText)
                                    showServerDialog = false
                                },
                            ) {
                                Text("Salvar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showServerDialog = false }) {
                                Text("Cancelar")
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceSection(
    themeMode: ThemeMode,
    appearance: Appearance,
    dynamicColor: Boolean,
    onThemeMode: (ThemeMode) -> Unit,
    onAppearance: (Appearance) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
) {
    SettingItem(
        icon = Icons.Default.DarkMode,
        title = "Tema",
        description =
            when (themeMode) {
                ThemeMode.LIGHT -> "Claro"
                ThemeMode.DARK -> "Escuro"
                ThemeMode.SYSTEM -> "Automático (sistema)"
            },
        trailing = {},
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = themeMode == ThemeMode.SYSTEM,
            onClick = { onThemeMode(ThemeMode.SYSTEM) },
            label = { Text("Automático") },
        )
        FilterChip(
            selected = themeMode == ThemeMode.LIGHT,
            onClick = { onThemeMode(ThemeMode.LIGHT) },
            label = { Text("Claro") },
        )
        FilterChip(
            selected = themeMode == ThemeMode.DARK,
            onClick = { onThemeMode(ThemeMode.DARK) },
            label = { Text("Escuro") },
        )
    }
    SettingItem(
        icon = Icons.Default.Palette,
        title = "Glassmorfismo",
        description =
            if (appearance == Appearance.GLASS) {
                "Cards translúcidos com borda de vidro"
            } else {
                "Cards sólidos"
            },
        trailing = {
            Switch(
                checked = appearance == Appearance.GLASS,
                onCheckedChange = {
                    onAppearance(if (it) Appearance.GLASS else Appearance.SOLID)
                },
            )
        },
    )
    SettingItem(
        icon = Icons.Default.Palette,
        title = "Cores dinâmicas",
        description = "Usar cores do papel de parede (Android 12+)",
        trailing = {
            Switch(
                checked = dynamicColor,
                onCheckedChange = onDynamicColor,
            )
        },
    )
}

// SettingItem e SettingsSection agora vivem no design system:
// com.example.lev.ui.components.SettingsItems
