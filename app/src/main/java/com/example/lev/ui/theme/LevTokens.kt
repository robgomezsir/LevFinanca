package com.example.lev.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Tokens da Fase 2 (sistema de design). Estrutura ADITIVA: nenhum símbolo de
 * [Color.kt] é alterado ou removido aqui — as telas reais continuam compilando
 * contra os valores atuais enquanto a migração por lotes acontece.
 *
 * Fonte da verdade da cor: quantização dos pixels de leve1.png / leve2.png.
 * Cores da marca medidas por amostragem e confirmadas:
 *   #008AC1 (wordmark, 197°) · #0D96FA (swoosh azul, 205°) · #2454B4 (ícone, 220°)
 *   #B3DDE4 (ciano pálido, 189°) · #0B9768 (folha, 160°) · ~#0A1116 (preto azulado).
 * Os degraus intermediários derivam dos mesmos eixos de matiz (sem matiz novo).
 *
 * Contraste verificado (WCAG 2.x, 4.5 texto / 3.0 UI) — relatório da Fase 2:
 *   onSurface claro 14.58 · primary claro 6.81 · income claro 6.99 · expense 7.24
 *   pending 7.91 · error 5.95 · accentInk claro 4.62 · outline claro 3.50 (UI)
 *   primary escuro 8.53 · income escuro 9.95 · expense escuro 8.88 · pending 12.00
 *   error escuro 11.20 · outline escuro 5.94 (UI)
 *
 * Regras de contraste que a tela deve respeitar:
 *   1. accent NUNCA é texto no tema claro (2.86:1); texto de ênfase usa [LevColors.accentInk].
 *      No escuro accent PODE ser texto (7.95:1).
 *   2. O ciano exato da marca (#008AC1) falha com texto branco por cima (3.88:1).
 *      O gradiente de superfície usa os stops cujo pior ponto mede ~4.88:1.
 *   3. Texto sobre o gradiente deve ser BRANCO SÓLIDO — branco com opacidade reduzida
 *      sobre #0079A8 fica em 3.75:1 e falha.
 *   4. outlineVariant é divisor decorativo; bordas FUNCIONAIS usam outline.
 */

// ---- Rampa Neutros (grafite azulado, ~200°, croma baixa) --------------------

val N0 = Color(0xFFFFFFFF)
val N50 = Color(0xFFF2F6F9)
val N100 = Color(0xFFE8EFF3)
val N150 = Color(0xFFDCE5EB)
val N200 = Color(0xFFC9D5DD)
val N300 = Color(0xFFA9B6C0)
val N400 = Color(0xFF83929D)
val N500 = Color(0xFF607181)
val N600 = Color(0xFF495A68)
val N700 = Color(0xFF36454F)
val N750 = Color(0xFF2A363D)
val N800 = Color(0xFF212C33)
val N850 = Color(0xFF121A20)
val N900 = Color(0xFF0F171C)
val N950 = Color(0xFF0A1116)
val N1000 = Color(0xFF05090C)

// ---- Rampa Marca (eixo do wordmark, 197°) ----------------------------------

val Brand100 = Color(0xFFC2E8F5)
val Brand200 = Color(0xFF8FD5EC)
val Brand300 = Color(0xFF4DB9E8) // = PrimaryDark atual (preservar valor)
val Brand400 = Color(0xFF1FA3CE)
val Brand500 = Color(0xFF008AC1) // MEDIDA da marca (leve2.png = 100% desta cor)
val Brand600 = Color(0xFF0079A8) // stop do gradiente seguro p/ texto branco (4.88:1)
val Brand700 = Color(0xFF005C7F)
val Brand800 = Color(0xFF004B68) // = PrimaryContainerDark atual
val Brand950 = Color(0xFF00293A)

// ---- Rampa Accent (swoosh azul vivo do logo, 205°) -------------------------

val Accent200 = Color(0xFFD6E9FE)
val Accent300 = Color(0xFF4FAEFB)
val Accent500 = Color(0xFF0D96FA) // MEDIDA do logo; FILL/seleção, nunca texto no claro
val Accent600 = Color(0xFF0B72C0) // accentInk claro: texto de ênfase (4.62:1)
val Accent800 = Color(0xFF06385F)
val Accent950 = Color(0xFF052F55)

// ---- Rampa Folha (verde do logo, 160°) — semântica de entrada/positivo -----

val Leaf200 = Color(0xFFA7F2D2)
val Leaf300 = Color(0xFF4FD1A5)
val Leaf500 = Color(0xFF0B9768) // MEDIDA da folha ("mais viva verde" do logo)
val Leaf600 = Color(0xFF097A54)
val Leaf700 = Color(0xFF076043)
val Leaf800 = Color(0xFF0A3D2C)
val Leaf950 = Color(0xFF00291B)

// ---- Stops do gradiente de superfície (fundo sob texto branco) -------------

val BrandGradientTokenStops = listOf(
    Color(0xFF2454B4), // índigo do ícone (6.99:1 com branco)
    Color(0xFF0079A8), // stop seguro no meio (4.88:1)
    Color(0xFF00658F), // stop do app atual (6.45:1)
)

/**
 * Tokens semânticos da nova direção (não são o `MaterialTheme.colorScheme`
 * atual — esse continua servido por [Theme.kt] até a migração tela a tela).
 */
data class LevColors(
    val canvas: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val accent: Color,
    val accentInk: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val income: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val pending: Color,
    val pendingContainer: Color,
    val onPendingContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val ledgerLine: Color,
    val onGradient: Color,
    val gradientStops: List<Color>,
)

val LightLevColors = LevColors(
    canvas = N50,
    surface = N0,
    surfaceAlt = N100,
    onSurface = Color(0xFF17242C),
    onSurfaceVariant = N600,
    primary = Brand700,
    onPrimary = N0,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand950,
    accent = Accent500,
    accentInk = Accent600,
    accentContainer = Accent200,
    onAccentContainer = Accent950,
    income = Leaf700,
    incomeContainer = Color(0xFFC8F0DE),
    onIncomeContainer = Leaf950,
    expense = Color(0xFFA50E0E),
    expenseContainer = Color(0xFFFFDAD6),
    onExpenseContainer = Color(0xFF410002),
    pending = Color(0xFF6E4200),
    pendingContainer = Color(0xFFFFDFA8),
    onPendingContainer = Color(0xFF2A1800),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF76858F),
    outlineVariant = N150,
    ledgerLine = Color(0xFFD3DEE4),
    onGradient = N0,
    gradientStops = BrandGradientTokenStops,
)

val DarkLevColors = LevColors(
    canvas = N950,
    surface = N850,
    surfaceAlt = N800,
    onSurface = Color(0xFFDCEFF5),
    onSurfaceVariant = Color(0xFFBFC9CE),
    primary = Brand300,
    onPrimary = Brand950,
    primaryContainer = Brand800,
    // Par aprovado/medido: #B3DDE4 sobre #004B68 = 6.52:1 (cian pálido do logo).
    onPrimaryContainer = Color(0xFFB3DDE4),
    accent = Accent300,
    accentInk = Accent300,
    accentContainer = Accent800,
    onAccentContainer = Accent200,
    income = Leaf300,
    incomeContainer = Leaf800,
    onIncomeContainer = Leaf200,
    expense = Color(0xFFFF9E99),
    expenseContainer = Color(0xFF5C1410),
    onExpenseContainer = Color(0xFFFFDAD6),
    pending = Color(0xFFFFC44D),
    pendingContainer = Color(0xFF4A3200),
    onPendingContainer = Color(0xFFFFE0A6),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = N400,
    outlineVariant = Color(0xFF4A575E),
    ledgerLine = N750,
    onGradient = N0,
    gradientStops = BrandGradientTokenStops,
)

/** Acesso aos tokens semânticos em qualquer composable do preview. */
val LocalLevColors = staticCompositionLocalOf { LightLevColors }
