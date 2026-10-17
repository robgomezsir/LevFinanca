package com.example.lev

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lev.ui.AppViewModelProvider
import com.example.lev.ui.components.LocalAppearance
import com.example.lev.ui.components.LocalIsDark
import com.example.lev.ui.navigation.LevApp
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.LevTheme
import com.example.lev.ui.theme.ThemeMode
import com.example.lev.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeViewModel: ThemeViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val themeState by themeViewModel.uiState.collectAsStateWithLifecycle()

            val darkTheme =
                when (themeState.mode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                }
            // Glassmorfismo respeita "remover animações"/contraste: o véu mantém
            // surfaceVariant por baixo nos cards (LevCard), só adiciona translucidez.
            val appearance =
                if (themeState.appearance == Appearance.GLASS) Appearance.GLASS else Appearance.SOLID

            LevTheme(themeMode = themeState.mode, dynamicColor = themeState.dynamicColor) {
                CompositionLocalProvider(
                    LocalAppearance provides appearance,
                    LocalIsDark provides darkTheme,
                ) {
                    LevApp(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
