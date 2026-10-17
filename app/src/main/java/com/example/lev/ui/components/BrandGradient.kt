package com.example.lev.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lev.ui.theme.Appearance
import com.example.lev.ui.theme.BrandSurfaceGradientColors
import com.example.lev.ui.theme.GlassEdgeDark
import com.example.lev.ui.theme.GlassEdgeLight
import com.example.lev.ui.theme.GlassVeilDark
import com.example.lev.ui.theme.GlassVeilLight
import com.example.lev.ui.theme.OnBrandGradient

/** Aparência ativa, fornecida na raiz do app a partir do ThemeViewModel. */
val LocalAppearance = compositionLocalOf { Appearance.SOLID }

/** Se o tema efetivo é escuro (resolve ThemeMode.SYSTEM contra o sistema). */
val LocalIsDark = compositionLocalOf { false }

@Composable
fun brandSurfaceBrush(): Brush = Brush.horizontalGradient(BrandSurfaceGradientColors)

/**
 * Card com o gradiente da marca. O conteúdo herda branco ([OnBrandGradient]),
 * então MoneyText com useSemanticColor=false e Text sem cor explícita ficam legíveis.
 */
@Composable
fun BrandGradientCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = modifier.background(brandSurfaceBrush(), MaterialTheme.shapes.extraLarge),
        ) {
            CompositionLocalProvider(LocalContentColor provides OnBrandGradient) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    } else {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            modifier = modifier.background(brandSurfaceBrush(), MaterialTheme.shapes.extraLarge),
        ) {
            CompositionLocalProvider(LocalContentColor provides OnBrandGradient) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    }
}

/**
 * Card que respeita a aparência: sólido (surfaceContainerHigh, com borda sutil de
 * hierarquia) ou vidro translúcido com borda. Um único raio por nível, na escala de
 * formas do tema (M3): extraLarge (24dp) no cartão de saldo, large (16dp) nos cartões
 * de conteúdo e CircleShape nas pílulas.
 */
@Composable
fun LevCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val appearance = LocalAppearance.current
    if (appearance == Appearance.GLASS) {
        val darkTheme = LocalIsDark.current
        val veil = if (darkTheme) GlassVeilDark else GlassVeilLight
        val edge = if (darkTheme) GlassEdgeDark else GlassEdgeLight
        // Uma demão só: o véu já vem no background — com containerColor igual, a alpha
        // era somada em duas camadas e quase anulava a translucidez do vidro.
        val glassModifier =
            modifier
                .clip(MaterialTheme.shapes.large)
                .background(veil)
                .border(1.dp, edge, MaterialTheme.shapes.large)
        if (onClick != null) {
            Card(
                onClick = onClick,
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = glassModifier,
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = glassModifier,
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    } else {
        if (onClick != null) {
            Card(
                onClick = onClick,
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                // Borda funcional em opacidade cheia (outlineVariant é apenas o
                // separador de divisor); profundidade vem do contraste
                // card-sobre-canvas, sem sombra.
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = modifier,
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        } else {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = modifier,
            ) {
                Column(modifier = Modifier.fillMaxWidth(), content = content)
            }
        }
    }
}

/** Traço de tinta do livro-caixa: o único uso decorativo do gradiente fora do topo. */
@Composable
fun GradientRule(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(CircleShape)
                .background(brandSurfaceBrush()),
    )
}

/** Título de seção do livro-caixa: Poiret One à esquerda, ação textual à direita. */
@Composable
fun LevSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            // Section header funcional: peso legível (SemiBold 16sp). O Poiret
            // One (traço de cabelo) ficou restrito a wordmark e título de tela.
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel, maxLines = 1) }
        }
    }
}

/** Faixa de fundo do cabeçalho com o gradiente da marca. */
@Composable
fun BrandHeaderBackground(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(brandSurfaceBrush()))
}

@Preview(showBackground = true, name = "Livro-caixa claro")
@Composable
private fun LevSystemLightPreview() {
    com.example.lev.ui.theme.LevTheme(themeMode = com.example.lev.ui.theme.ThemeMode.LIGHT) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevSectionHeader(title = "Outras contas", actionLabel = "Ver contas", onAction = {})
            LevCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "R$ 1.234,56",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(20.dp),
                )
            }
            GradientRule()
        }
    }
}

@Preview(
    showBackground = true,
    name = "Livro-caixa escuro",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun LevSystemDarkPreview() {
    com.example.lev.ui.theme.LevTheme(themeMode = com.example.lev.ui.theme.ThemeMode.DARK) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevSectionHeader(title = "Outras contas", actionLabel = "Ver contas", onAction = {})
            LevCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "R$ 1.234,56",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(20.dp),
                )
            }
            GradientRule()
        }
    }
}

@Preview(showBackground = true, name = "Livro-caixa fonte grande", fontScale = 2f)
@Composable
private fun LevSystemLargeFontPreview() {
    com.example.lev.ui.theme.LevTheme(themeMode = com.example.lev.ui.theme.ThemeMode.LIGHT) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevSectionHeader(title = "Outras contas", actionLabel = "Ver contas", onAction = {})
            LevCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "R$ 1.234,56",
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier.padding(20.dp),
                )
            }
            GradientRule()
        }
    }
}
