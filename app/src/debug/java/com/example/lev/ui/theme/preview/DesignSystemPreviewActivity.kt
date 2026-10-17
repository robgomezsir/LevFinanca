package com.example.lev.ui.theme.preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Hosteia a tela-piloto do design system. Definida SOMENTE no source set
 * debug (manifest em app/src/debug) — não existe no build release e não entra
 * na navegação real do app. Acesso via `am start` explícito:
 *
 * adb shell am start -n com.example.lev/com.example.lev.ui.theme.preview.DesignSystemPreviewActivity
 *
 * Extra opcional: dark (Boolean) para abrir o tema escuro diretamente.
 */
class DesignSystemPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dark = intent?.getBooleanExtra(DARK_EXTRA, false) ?: false
        setContent { DesignSystemPreviewScreen(initialDark = dark) }
    }

    private companion object {
        const val DARK_EXTRA = "dark"
    }
}
