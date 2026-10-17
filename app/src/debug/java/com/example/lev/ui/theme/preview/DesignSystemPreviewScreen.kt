package com.example.lev.ui.theme.preview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lev.ui.theme.LocalLevColors
import com.example.lev.ui.theme.LevSpacing

/**
 * Tela-piloto da Fase 2: vitrine read-only do sistema de design proposto.
 * Não é rota do app; aberta somente pela DesignSystemPreviewActivity (debug).
 *
 * Blocos: rampas da marca, semântica, tipografia, formas/escala, componentes,
 * gradiente (com a regra de texto branco sólido) e regras de contraste medidas.
 */
@Composable
fun DesignSystemPreviewScreen(initialDark: Boolean) {
    var dark by remember { mutableStateOf(initialDark) }
    PreviewTheme(darkTheme = dark) {
        val tokens = LocalLevColors.current
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .safeDrawingPadding(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LevSpacing.LG, vertical = LevSpacing.SM),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "leve",
                    style = MaterialTheme.typography.headlineLarge,
                    color = tokens.onSurface,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { dark = false }) { Text("Claro") }
                    TextButton(onClick = { dark = true }) { Text("Escuro") }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = LevSpacing.XXL),
            ) {
                item { SpecSection(title = "Cores da marca") }
                item { BrandSwatches() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Semântica") }
                item { SemanticSamples() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Tipografia") }
                item { TypeSamples() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Formas e escala") }
                item { ShapeAndSpacingSamples() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Componentes") }
                item { ComponentSamples() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Gradiente da marca (regra de texto)") }
                item { GradientSample() }
                item { Spacer(Modifier.height(LevSpacing.LG)) }
                item { SpecSection(title = "Regras de contraste (WCAG 2.x)") }
                item { RulesSample() }
                item { Spacer(Modifier.height(LevSpacing.XL)) }
            }
        }
    }
}

@Composable
private fun SpecSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = LevSpacing.LG,
                    end = LevSpacing.LG,
                    top = LevSpacing.SM,
                    bottom = 4.dp,
                ),
    )
}

/** Card da proposta Fase 3: fundo branco, borda 1dx sutil, sem sombra. */
@Composable
private fun SpecCard(content: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun BrandSwatches() {
    SpecCard {
        Column(modifier = Modifier.padding(horizontal = LevSpacing.MD, vertical = LevSpacing.SM)) {
            SwatchRow(
                label = "Brand (197° · wordmark medido #008AC1)",
                swatches =
                    listOf(
                        "100" to Color(0xFFC2E8F5),
                        "300" to Color(0xFF4DB9E8),
                        "500" to Color(0xFF008AC1),
                        "700" to Color(0xFF005C7F),
                        "950" to Color(0xFF00293A),
                    ),
            )
            SwatchRow(
                label = "Accent (205° · swoosh do logo)",
                swatches =
                    listOf(
                        "200" to Color(0xFFD6E9FE),
                        "300" to Color(0xFF4FAEFB),
                        "500" to Color(0xFF0D96FA),
                        "600" to Color(0xFF0B72C0),
                        "950" to Color(0xFF052F55),
                    ),
            )
            SwatchRow(
                label = "Leaf (160° · folha do logo, semântica de entrada)",
                swatches =
                    listOf(
                        "200" to Color(0xFFA7F2D2),
                        "300" to Color(0xFF4FD1A5),
                        "500" to Color(0xFF0B9768),
                        "700" to Color(0xFF076043),
                        "950" to Color(0xFF00291B),
                    ),
            )
            SwatchRow(
                label = "Neutros (grafite azulado)",
                swatches =
                    listOf(
                        "50" to Color(0xFFF2F6F9),
                        "150" to Color(0xFFDCE5EB),
                        "400" to Color(0xFF83929D),
                        "850" to Color(0xFF121A20),
                        "950" to Color(0xFF0A1116),
                    ),
            )
            Text(
                text = "Sem matiz novo fora dos eixos do logo.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** Rampa horizontal: cada degrau em caixinha nomeada. */
@Composable
private fun SwatchRow(label: String, swatches: List<Pair<String, Color>>) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = LevSpacing.XS),
    )
    Spacer(Modifier.height(4.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(LevSpacing.XS)) {
        items(swatches) { (name, color) ->
            Box(
                modifier =
                    Modifier
                        .height(28.dp)
                        .width(52.dp)
                        .background(color, RoundedCornerShape(6.dp))
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    // Label interno: branco sobre degraus escuros, escuro sobre claros.
                    color =
                        if (color.luminance() < 0.35f) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
    Spacer(Modifier.height(LevSpacing.XS))
}

@Composable
private fun SemanticSamples() {
    val tokens = LocalLevColors.current
    SpecCard {
        Column(modifier = Modifier.padding(LevSpacing.MD)) {
            SemanticRow(
                label = "Entrada (folha)",
                container = tokens.incomeContainer,
                ink = tokens.income,
                value = "+ R$ 1.234,56",
            )
            SemanticRow(
                label = "Saída",
                container = tokens.expenseContainer,
                ink = tokens.expense,
                value = "− R$ 456,00",
            )
            SemanticRow(
                label = "Pendente",
                container = tokens.pendingContainer,
                ink = tokens.pending,
                value = "Previsto",
            )
            SemanticRow(
                label = "Erro",
                container = tokens.errorContainer,
                ink = tokens.error,
                value = "Falha ao salvar",
            )
            Spacer(Modifier.height(LevSpacing.SM))
            Text(
                text = "Badge primário em primaryContainer (11.72:1):",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(50))
                        .padding(horizontal = LevSpacing.MD, vertical = 4.dp),
            ) {
                Text(
                    text = "Conta corrente",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun SemanticRow(label: String, container: Color, ink: Color, value: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(110.dp),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .background(container, RoundedCornerShape(6.dp))
                    .padding(horizontal = LevSpacing.SM, vertical = 4.dp),
        ) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = ink)
        }
    }
}

@Composable
private fun TypeSamples() {
    SpecCard {
        Column(modifier = Modifier.padding(LevSpacing.MD)) {
            TypeRow(name = "displaySmall (tnum)", sample = "R$ 12.345,67", style = MaterialTheme.typography.displaySmall)
            TypeRow(name = "headlineLarge (Poiret One)", sample = "Transações", style = MaterialTheme.typography.headlineLarge)
            TypeRow(name = "headlineSmall (Poiret One)", sample = "Outras contas", style = MaterialTheme.typography.headlineSmall)
            TypeRow(name = "titleLarge", sample = "Confirmar entrada", style = MaterialTheme.typography.titleLarge)
            TypeRow(name = "titleMedium", sample = "Orçamento mensal", style = MaterialTheme.typography.titleMedium)
            TypeRow(name = "bodyLarge (tnum)", sample = "Saldo da conta", style = MaterialTheme.typography.bodyLarge)
            TypeRow(name = "bodyMedium (tnum)", sample = "Cartão Nubank", style = MaterialTheme.typography.bodyMedium)
            TypeRow(name = "labelLarge", sample = "Salvar", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "Proposta Fase 3: Poiret One só em wordmark + título de tela. " +
                    "Os valores usam tabularTextStyle (tnum).",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun TypeRow(name: String, sample: String, style: androidx.compose.ui.text.TextStyle) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(184.dp),
        )
        Text(text = sample, style = style, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ShapeAndSpacingSamples() {
    val tokens = LocalLevColors.current
    SpecCard {
        Column(modifier = Modifier.padding(LevSpacing.MD)) {
            Row(horizontalArrangement = Arrangement.spacedBy(LevSpacing.SM)) {
                listOf(4, 10, 14, 20, 24).forEach { r ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier =
                                Modifier
                                    .height(40.dp)
                                    .width(40.dp)
                                    .background(tokens.surfaceAlt, RoundedCornerShape(r.dp))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(r.dp)),
                        )
                        Text(text = "$r dp", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
            }
            Spacer(Modifier.height(LevSpacing.MD))
            listOf(4, 8, 12, 16, 20, 24, 32, 40).forEach { v ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = "$v",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp),
                    )
                    Spacer(Modifier.width(LevSpacing.SM))
                    Box(
                        modifier =
                            Modifier
                                .height(8.dp)
                                .width(v.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentSamples() {
    val tokens = LocalLevColors.current
    Column(
        modifier = Modifier.padding(horizontal = LevSpacing.LG),
        verticalArrangement = Arrangement.spacedBy(LevSpacing.SM),
    ) {
        Text(text = "Botões", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(LevSpacing.SM)) {
            Button(onClick = {}) { Text("Primário") }
            FilledTonalButton(onClick = {}) { Text("Tonal") }
            OutlinedButton(onClick = {}) { Text("Contorno") }
        }
        Spacer(Modifier.height(LevSpacing.XS))
        Text(text = "Card da proposta (branco + borda 1 dp, sem sombra)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SpecCard {
            Column(modifier = Modifier.padding(LevSpacing.MD)) {
                Text(text = "Cartão de crédito", style = MaterialTheme.typography.titleMedium, color = tokens.onSurface)
                Text(
                    text = "surfaceContainerHigh (branco) sobre canvas neutro 50. " +
                        "Sombra apenas em FAB, bottom sheet e diálogo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(LevSpacing.XS))
        Text(text = "Progresso", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LevSpacing.MD)) {
            // Overload clássico (progress: Float) por compatibilidade com esta BOM.
            LinearProgressIndicator(
                progress = 0.6f,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f),
            )
            CircularProgressIndicator(
                progress = 0.75f,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
        Spacer(Modifier.height(LevSpacing.XS))
        Text(text = "Lista (linha do livro-caixa)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = LevSizeRow),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Mercado", style = MaterialTheme.typography.bodyMedium, color = tokens.onSurface)
            Text("− R$ 178,90", style = MaterialTheme.typography.bodyMedium, color = tokens.expense)
        }
        Divider(color = tokens.ledgerLine, thickness = 1.dp)
    }
}

private val LevSizeRow = 48.dp

@Composable
private fun GradientSample() {
    val tokens = LocalLevColors.current
    Column(modifier = Modifier.padding(horizontal = LevSpacing.LG)) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .background(Brush.horizontalGradient(tokens.gradientStops), MaterialTheme.shapes.large),
        ) {
            Column(modifier = Modifier.padding(LevSpacing.LG)) {
                Text(text = "Saldo total", style = MaterialTheme.typography.labelLarge, color = tokens.onGradient)
                Spacer(Modifier.height(4.dp))
                Text(text = "R$ 12.345,67", style = MaterialTheme.typography.displaySmall, color = tokens.onGradient)
            }
        }
        Text(
            text = "Stops #2454B4 → #0079A8 → #00658F · pior ponto com branco: 4.88:1. " +
                "Texto sobre gradiente SEMPRE branco sólido (branco 80% mede 3.75 e falha).",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = LevSpacing.SM),
        )
    }
}

@Composable
private fun RulesSample() {
    listOf(
        "onSurface · 14.58:1 claro / 16.03:1 escuro",
        "primary · 6.81:1 claro / 8.53:1 escuro",
        "income (folha) · 6.99:1 claro / 9.95:1 escuro",
        "expense · 7.24:1 claro / 8.88:1 escuro",
        "pending · 7.91:1 claro / 12.00:1 escuro",
        "accent como TEXTO no claro = 2.86:1 (FALHA) → accentInk 4.62:1",
        "outline claro #76858F = 3.50:1 (UI ≥ 3.0)",
        "outlineVariant escuro = divisor decorativo (isento)",
        "branco 80% sobre #0079A8 = 3.75:1 (proibido no gradiente)",
    ).forEach { note ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
            Text(
                text = "•",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = LevSpacing.SM),
            )
            Text(text = note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
