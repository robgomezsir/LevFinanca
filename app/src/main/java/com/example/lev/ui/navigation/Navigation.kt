package com.example.lev.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lev.LevApplication
import com.example.lev.ui.AppViewModelProvider
import com.example.lev.ui.screens.MainScreen
import com.example.lev.ui.screens.onboarding.OnboardingScreen
import com.example.lev.ui.screens.onboarding.OnboardingViewModel

/**
 * Ponto de entrada do app: resolve a primeira tela pelo onboarding persistido antes de montar o
 * [LevNavHost], para que abrir o app depois de concluído não caia no onboarding de novo.
 *
 * O grafo raiz tem só onboarding/main. Todas as rotas internas (inclusive fatura)
 * vivem no NavController próprio do MainScreen, em back-stack único.
 */
@Composable
fun LevApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: StartDestinationViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    startDestination?.let { route ->
        LevNavHost(
            modifier = modifier,
            navController = navController,
            startDestination = route,
        )
    }
}

@Composable
fun LevNavHost(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(Screen.Onboarding.route) {
            val viewModel: OnboardingViewModel = viewModel(factory = AppViewModelProvider.Factory)
            OnboardingScreen(
                viewModel = viewModel,
                onFinished = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Main.route) {
            val context = LocalContext.current
            // Container é resolvido dentro do MainScreen pelos ViewModels; aqui só repassamos
            // o repositório para telas que ainda usam acesso direto, via applicationContext.

            @Suppress("UNUSED_VARIABLE")
            val app = context.applicationContext as LevApplication
            MainScreen(modifier = modifier)
        }
    }
}
