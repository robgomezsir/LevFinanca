package com.example.lev.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escalas dimensionais da Fase 2. Grade de 4dp; nenhum valor "mágico" fora da
 * escala. Aplicada tela a tela na Fase 3 — nada aqui é lido pelas telas reais.
 */
object LevSpacing {
    val XS = 4.dp
    val SM = 8.dp
    val MD = 12.dp
    val LG = 16.dp
    val XL = 20.dp
    val XXL = 24.dp
    val XXXL = 32.dp
    val JUMBO = 40.dp

    /** Espessura padrão de padding lateral de tela. */
    val screenPadding: Dp = LG

    /** Gutter entre cards / list rows. */
    val gutter: Dp = MD
}

/** Tamanhos canônicos de ícones, toque e componentes. */
object LevSize {
    val IconSM = 18.dp
    val IconMD = 20.dp
    val IconLG = 24.dp

    /** Alvo mínimo de toque (Material accessibility). */
    val TouchTarget = 48.dp

    val Fab = 56.dp

    /** Altura mínima de list row (acc source of truth p/ [LevSize.TouchTarget]). */
    val ListRow = 48.dp

    /** Raio de topo do bottom sheet, já representando "M3 large". */
    val SheetTopRadius = 20.dp
}
