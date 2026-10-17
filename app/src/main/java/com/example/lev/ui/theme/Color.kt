package com.example.lev.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta da Fase 3 (aprovada): derivada do logo (quantização de leve1.png /
// leve2.png). Eixos: marca 197 (#008AC1 wordmark), accent 205 (#0D96FA swoosh),
// folha 160 (#0B9768) e neutros grafite azulado. Contraste verificado por WCAG
// 2.x (4.5 texto / 3.0 UI/ícone). Os NOMES públicos não mudam — migração tela a
// tela usa apenas novos VALORES.

// ---- Claro (canvas grafite n50, cards brancos n0) ----
val PrimaryLight = Color(0xFF005C7F) // brand700 — 6.81:1 sobre canvas
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFC2E8F5) // brand100
val OnPrimaryContainerLight = Color(0xFF00293A) // brand950 — 11.72:1
val SecondaryLight = Color(0xFF495A68) // n600, ardósia neutra (7.31:1)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE8EFF3) // n100
val OnSecondaryContainerLight = Color(0xFF17242C)
val TertiaryLight = Color(0xFF0B72C0) // accentInk (4.62:1) — ênfase textual
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFD6E9FE) // accent200
val OnTertiaryContainerLight = Color(0xFF052F55) // accent950 — 10.97:1
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFF2F6F9) // canvas grafite n50
val OnBackgroundLight = Color(0xFF17242C) // 14.58:1
val SurfaceLight = Color(0xFFF2F6F9) // Scaffold continua no canvas (cards são n0)
val OnSurfaceLight = Color(0xFF17242C)
val SurfaceVariantLight = Color(0xFFE8EFF3) // n100 (campos/tonais)
val OnSurfaceVariantLight = Color(0xFF495A68) // n600 — 7.31:1
val SurfaceContainerLowestLight = Color(0xFFFFFFFF) // cartão branco
val SurfaceContainerLowLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFFFFFFF)
val SurfaceContainerHighLight = Color(0xFFFFFFFF) // LevCard/sheets brancos
val SurfaceContainerHighestLight = Color(0xFFDCE5EB) // n150 (press/tonal)
val OutlineLight = Color(0xFF76858F) // borda FUNCIONAL — 3.50:1 (≥3.0)
val OutlineVariantLight = Color(0xFFDCE5EB) // divisor DECORATIVO

// ---- Escuro (canvas preto azulado do logo n950, cards n850) ----
val PrimaryDark = Color(0xFF4DB9E8) // brand300 — 8.53:1
val OnPrimaryDark = Color(0xFF00293A)
val PrimaryContainerDark = Color(0xFF004B68) // brand800
val OnPrimaryContainerDark = Color(0xFFB3DDE4) // ciano pálido do logo — 6.52:1
val SecondaryDark = Color(0xFFA9B6C0) // n300 neutro
val OnSecondaryDark = Color(0xFF0A1116)
val SecondaryContainerDark = Color(0xFF212C33) // n800
val OnSecondaryContainerDark = Color(0xFFBFC9CE)
val TertiaryDark = Color(0xFF4FAEFB) // accent300 — 7.95:1 (accent é texto no escuro)
val OnTertiaryDark = Color(0xFF0A1116)
val TertiaryContainerDark = Color(0xFF06385F) // accent800
val OnTertiaryContainerDark = Color(0xFFD6E9FE) // accent200 — 9.75:1
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val BackgroundDark = Color(0xFF0A1116) // n950 — preto azulado do logo
val OnBackgroundDark = Color(0xFFDCEFF5) // 16.03:1
val SurfaceDark = Color(0xFF0A1116)
val OnSurfaceDark = Color(0xFFDCEFF5)
val SurfaceVariantDark = Color(0xFF212C33) // n800 (campos/tonais)
val OnSurfaceVariantDark = Color(0xFFBFC9CE) // 11.28:1
val SurfaceContainerLowestDark = Color(0xFF05090C)
val SurfaceContainerLowDark = Color(0xFF121A20) // cartão n850
val SurfaceContainerDark = Color(0xFF121A20)
val SurfaceContainerHighDark = Color(0xFF121A20) // LevCard/sheets
val SurfaceContainerHighestDark = Color(0xFF212C33) // n800 (press/tonal)
val OutlineDark = Color(0xFF83929D) // borda FUNCIONAL — 5.94:1
val OutlineVariantDark = Color(0xFF4A575E) // divisor decorativo (isento)

// Semantic Tokens — variantes claro/escuro com contraste AA (>= 4.5:1).
// Verba/entrada usa o verde da FOLHA do logo (160°), não mais verde genérico:
// IncomeLight #076043 (6.99:1) / IncomeDark #4FD1A5 (9.95:1).
// Despesas usam "−" + posição, nunca só cor (Base §7).
val IncomeLight = Color(0xFF076043)
val IncomeDark = Color(0xFF4FD1A5)
val ExpenseLight = Color(0xFFA50E0E) // 7.24:1
val ExpenseDark = Color(0xFFFF9E99) // 8.88:1
val PendingLight = Color(0xFF6E4200) // 7.91:1
val PendingDark = Color(0xFFFFC44D) // 12.00:1
val OverdueLight = ExpenseLight
val OverdueDark = ExpenseDark
val TransferLight = Color(0xFF2454B4)
val TransferDark = Color(0xFFA9BBF2)

// Aliases legados (telas que ainda não migraram para claro/escuro explícito)
val IncomeColor = IncomeLight
val ExpenseColor = ExpenseLight
val PendingColor = PendingLight
val OverdueColor = OverdueLight
val TransferColor = TransferLight

// Linha do livro-caixa: separa lançamentos sem virar card
val LedgerLineLight = Color(0xFFD3DEE4)
val LedgerLineDark = Color(0xFF2A363D)

// Gradiente da marca, na ordem em que aparece no logo (só decorativo)
val BrandGradientColors = listOf(Color(0xFF2454B4), Color(0xFF008FC7), Color(0xFFB3DDE4))

// Gradiente aplicado sobre superfícies (cabeçalho, card de saldo): os stops
// passam em AA com texto branco em QUALQUER ponto da faixa (pior amostra medida:
// 4,88:1 em #0079A8). Regra: texto sobre o gradiente é BRANCO SÓLIDO — com
// opacidade reduzida a parada do meio mede 3,75:1 e falha.
val BrandSurfaceGradientColors = listOf(Color(0xFF2454B4), Color(0xFF0079A8), Color(0xFF00658F))

// Conteúdo sobre o gradiente da marca
val OnBrandGradient = Color(0xFFFFFFFF)

// Vidro (glassmorfismo): véu claro sobre a superfície + borda translúcida.
// Feature existente do usuário (Appearance.GLASS) — preservada.
val GlassVeilLight = Color(0xB3FFFFFF)
val GlassEdgeLight = Color(0x6600A9D6)
val GlassVeilDark = Color(0x4D1B252A)
val GlassEdgeDark = Color(0x664DB9E8)
