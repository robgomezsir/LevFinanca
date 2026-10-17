package com.example.lev.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.lev.ui.components.EmptyState
import com.example.lev.ui.components.GradientRule
import com.example.lev.ui.components.LevCard
import com.example.lev.ui.components.LevProgressBar
import com.example.lev.ui.components.LevSectionHeader
import com.example.lev.ui.components.LocalAppearance
import com.example.lev.ui.components.MoneyText
import com.example.lev.ui.screens.dashboard.BalanceSummaryCard
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.LevTheme
import com.example.lev.ui.theme.ThemeMode
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots do design system livro-caixa: claro, escuro, vidro claro/escuro e fonte grande.
 * Gerar referências: `./gradlew :app:recordRoborazziDebug`
 * Verificar: `./gradlew :app:verifyRoborazziDebug`
 *
 * Usa um Application neutro em vez do grafo de produção: o onCreate de produção constrói o
 * SyncRepository, que consulta Room numa Dispatchers.IO de fundo — sob Robolectric (SQLite
 * legado com ponteiros por thread) essa coroutine cruza a troca de ambiente entre testes e
 * derrubava o teste seguinte com UncaughtExceptionsBeforeTest. Este teste só renderiza
 * composables do design system, sem dependência do container.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class LevDesignScreenshotsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun designSystem_claro() {
        composeRule.setContent {
            LevTheme(themeMode = ThemeMode.LIGHT) { DesignSample() }
        }
        settleAndCapture()
    }

    @Test
    fun designSystem_escuro() {
        composeRule.setContent {
            LevTheme(themeMode = ThemeMode.DARK) { DesignSample() }
        }
        settleAndCapture()
    }

    @Test
    fun designSystem_vidroClaro() {
        composeRule.setContent {
            LevTheme(themeMode = ThemeMode.LIGHT) {
                CompositionLocalProvider(LocalAppearance provides Appearance.GLASS) {
                    DesignSample()
                }
            }
        }
        settleAndCapture()
    }

    @Test
    fun designSystem_vidroEscuro() {
        composeRule.setContent {
            LevTheme(themeMode = ThemeMode.DARK) {
                CompositionLocalProvider(LocalAppearance provides Appearance.GLASS) {
                    DesignSample()
                }
            }
        }
        settleAndCapture()
    }

    @Test
    fun designSystem_fonteGrande() {
        composeRule.setContent {
            LevTheme(themeMode = ThemeMode.LIGHT) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                ) {
                    DesignSample()
                }
            }
        }
        settleAndCapture()
    }

    /**
     * Assenta animações (barra de progresso) e o relógio do Compose antes de
     * capturar, para a imagem ser determinística e nenhuma coroutine vazar
     * para o teste seguinte.
     */
    private fun settleAndCapture() {
        composeRule.mainClock.advanceTimeBy(1_000L)
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
        composeRule.waitForIdle()
    }
}

@Composable
private fun DesignSample() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BalanceSummaryCard(
            totalBalanceCents = 123456,
            totalCardDebtCents = 23400,
            onClick = {},
        )
        LevSectionHeader(title = "Outras contas", actionLabel = "Ver contas", onAction = {})
        LevCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Saldo total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                MoneyText(cents = 123456, style = MaterialTheme.typography.displaySmall, useSemanticColor = false)
                GradientRule()
                MoneyText(
                    cents = -23400,
                    style = MaterialTheme.typography.bodyLarge,
                    useSemanticColor = true,
                    showSign = true,
                )
            }
        }
        LevProgressBar(fraction = 0.72f, label = "72% usado", contentDescription = "Orçamento 72 por cento usado")
        EmptyState(
            title = "Sem relatórios ainda",
            description = "Adicione lançamentos para ver patrimônio, orçamento e insights.",
        )
    }
}
