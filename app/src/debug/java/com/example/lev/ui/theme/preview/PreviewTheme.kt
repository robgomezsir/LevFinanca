package com.example.lev.ui.theme.preview

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.lev.ui.theme.DarkLevColors
import com.example.lev.ui.theme.ErrorContainerDark
import com.example.lev.ui.theme.LightLevColors
import com.example.lev.ui.theme.LocalLevColors
import com.example.lev.ui.theme.Shapes
import com.example.lev.ui.theme.Typography

/**
 * Tema do PREVIEW (somente debug — vive em app/src/debug). Servido do
 * [LocalLevColors] + ColorScheme M3 derivado. NÃO substitui `Theme.kt`:
 * o `LevTheme` real continua servindo as telas atuais até a Fase 3 migrar
 * por lotes; aqui é a vitrine da proposta.
 */
@Composable
fun PreviewTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val tokens =
        if (darkTheme) {
            DarkLevColors
        } else {
            LightLevColors
        }
    val scheme =
        if (darkTheme) {
            darkColorScheme(
                primary = tokens.primary,
                onPrimary = tokens.onPrimary,
                primaryContainer = tokens.primaryContainer,
                onPrimaryContainer = tokens.onPrimaryContainer,
                secondary = tokens.primary,
                onSecondary = tokens.onPrimary,
                secondaryContainer = tokens.primaryContainer,
                onSecondaryContainer = tokens.onPrimaryContainer,
                tertiary = tokens.accentInk,
                onTertiary = tokens.onPrimary,
                background = tokens.canvas,
                onBackground = tokens.onSurface,
                surface = tokens.surface,
                onSurface = tokens.onSurface,
                surfaceVariant = tokens.surfaceAlt,
                onSurfaceVariant = tokens.onSurfaceVariant,
                surfaceContainerLowest = tokens.canvas,
                surfaceContainerLow = tokens.canvas,
                surfaceContainer = tokens.surface,
                surfaceContainerHigh = tokens.surface,
                surfaceContainerHighest = tokens.surfaceAlt,
                outline = tokens.outline,
                outlineVariant = tokens.outlineVariant,
                error = tokens.error,
                onError = tokens.onPrimary,
                errorContainer = ErrorContainerDark,
                onErrorContainer = tokens.onErrorContainer,
            )
        } else {
            lightColorScheme(
                primary = tokens.primary,
                onPrimary = tokens.onPrimary,
                primaryContainer = tokens.primaryContainer,
                onPrimaryContainer = tokens.onPrimaryContainer,
                secondary = tokens.primary,
                onSecondary = tokens.onPrimary,
                secondaryContainer = tokens.primaryContainer,
                onSecondaryContainer = tokens.onPrimaryContainer,
                tertiary = tokens.accentInk,
                onTertiary = tokens.onPrimary,
                background = tokens.canvas,
                onBackground = tokens.onSurface,
                surface = tokens.surface,
                onSurface = tokens.onSurface,
                surfaceVariant = tokens.surfaceAlt,
                onSurfaceVariant = tokens.onSurfaceVariant,
                surfaceContainerLowest = tokens.surface,
                surfaceContainerLow = tokens.surface,
                surfaceContainer = tokens.surface,
                surfaceContainerHigh = tokens.surface,
                surfaceContainerHighest = tokens.surfaceAlt,
                outline = tokens.outline,
                outlineVariant = tokens.outlineVariant,
                error = tokens.error,
                onError = tokens.onPrimary,
                errorContainer = tokens.errorContainer,
                onErrorContainer = tokens.onErrorContainer,
            )
        }
    CompositionLocalProvider(LocalLevColors provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography,
            shapes = Shapes,
            content = content,
        )
    }
}
