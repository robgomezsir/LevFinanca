package com.example.lev.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lev.ui.theme.LevTheme

/**
 * Lê "Remover animações" do sistema: escala de animação zerada = sem movimento.
 * Usado para respeitar a preferência em todas as animações não solicitadas.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Barra de progresso do livro-caixa: trilha sutil, preenchimento em tinta sólida,
 * rótulo textual (opção "ver como tabela" embutida) e semântica de progresso
 * para leitores de tela.
 */
@Composable
fun LevProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    label: String? = null,
    contentDescription: String? = null,
) {
    val clamped = fraction.coerceIn(0f, 1f)
    val reduceMotion = rememberReducedMotion()
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = if (reduceMotion) 0 else 500, easing = FastOutSlowInEasing),
        label = "lev_progress",
    )
    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = if (label != null) 4.dp else 0.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .semantics(mergeDescendants = true) {
                        progressBarRangeInfo =
                            androidx.compose.ui.semantics.ProgressBarRangeInfo(
                                current = clamped,
                                range = 0f..1f,
                            )
                        if (contentDescription != null) this.contentDescription = contentDescription
                    },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .background(color, CircleShape),
            )
        }
    }
}

/**
 * Valor monetário com contagem animada na primeira exibição — o único momento
 * orquestrado da tela. Com "Remover animações" ativo, mostra o valor final
 * direto. O TalkBack sempre lê o valor final.
 */
@Composable
fun AnimatedMoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displaySmall,
    useSemanticColor: Boolean = false,
) {
    val reduceMotion = rememberReducedMotion()
    val progress: Animatable<Float, AnimationVector1D> = remember { Animatable(0f) }
    LaunchedEffect(cents, reduceMotion) {
        if (reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
        }
    }
    MoneyText(
        cents = (cents * progress.value).toLong(),
        modifier = modifier,
        style = style,
        useSemanticColor = useSemanticColor,
    )
}

@Preview(showBackground = true, name = "Progresso claro")
@Composable
private fun LevProgressPreview() {
    LevTheme(themeMode = com.example.lev.ui.theme.ThemeMode.LIGHT) {
        LevProgressBar(fraction = 0.72f, label = "72% usado", contentDescription = "Orçamento 72 por cento usado")
    }
}

@Preview(showBackground = true, name = "Progresso escuro", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LevProgressDarkPreview() {
    LevTheme(themeMode = com.example.lev.ui.theme.ThemeMode.DARK) {
        LevProgressBar(fraction = 0.72f, label = "72% usado", contentDescription = "Orçamento 72 por cento usado")
    }
}
