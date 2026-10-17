package com.example.lev.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    onFinished: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) {
            onFinished()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (uiState.step) {
                0 -> WelcomeStep(onNext = { viewModel.nextStep() })
                1 -> CategoriesStep(onNext = { viewModel.nextStep() })
                2 -> CreateAccountStep(onNext = { viewModel.nextStep() })
                3 ->
                    ConnectSheetsStep(
                        onFinish = { accountName, balance, sheetsUrl, sheetsSecret ->
                            viewModel.finishOnboarding(accountName, balance, sheetsUrl, sheetsSecret)
                        },
                    )
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Text(text = "Bem-vindo ao leve", style = MaterialTheme.typography.headlineLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Seu novo app de finanças pessoais.\nOffline-first, rápido e privado.",
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onNext) { Text("Começar") }
}

@Composable
fun CategoriesStep(onNext: () -> Unit) {
    Text(text = "Categorias Prontas", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Já preparamos categorias como Salário, Mercado e Lazer para você. Você poderá personalizá-las depois.",
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onNext) { Text("Continuar") }
}

@Composable
fun CreateAccountStep(onNext: () -> Unit) {
    Text(text = "Primeira Conta", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Vamos criar sua primeira conta.\n(Você definirá os detalhes na próxima tela)",
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onNext) { Text("Continuar") }
}

@Composable
fun ConnectSheetsStep(onFinish: (String, Long, String, String) -> Unit) {
    var accountName by remember { mutableStateOf("") }
    var sheetsUrl by remember { mutableStateOf("") }
    var sheetsSecret by remember { mutableStateOf("") }

    Text(text = "Configuração Inicial", style = MaterialTheme.typography.headlineMedium)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedTextField(
        value = accountName,
        onValueChange = { accountName = it },
        label = { Text("Nome da Conta (ex: Carteira, Nubank)") },
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text("Conectar Planilha (Backup)", style = MaterialTheme.typography.titleMedium)
    Text(
        text = "Opcional — a planilha é espelho e backup. Pode pular e conectar depois em Configurações > Planilha.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = sheetsUrl,
        onValueChange = { sheetsUrl = it },
        label = { Text("URL /exec (opcional)") },
        singleLine = true,
    )
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = sheetsSecret,
        onValueChange = { sheetsSecret = it },
        label = { Text("Segredo (opcional)") },
        singleLine = true,
    )

    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { onFinish(accountName, 0L, sheetsUrl.trim(), sheetsSecret) }) {
        Text("Concluir e Ir para o Painel")
    }
}
