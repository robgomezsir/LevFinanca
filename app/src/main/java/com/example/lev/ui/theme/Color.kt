package com.example.lev.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta derivada do logo da marca (res/drawable/leve1.png): gradiente azul-índigo
// (#2454B4) -> azul-ciano vivo (#008FC7) -> ciano pálido (#B3DDE4) sobre preto azulado.
// Extraída por quantização dos pixels do próprio PNG.

// ---- Claro ----
val PrimaryLight = Color(0xFF00658F) // ciano-azulado escurecido (AA sobre branco)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFB3DDE4) // extremo claro do gradiente do logo
val OnPrimaryContainerLight = Color(0xFF00293A)
val SecondaryLight = Color(0xFF4E7086) // ardósia azulada, dessaturada
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFCFE7F2)
val OnSecondaryContainerLight = Color(0xFF0B2B3A)
val TertiaryLight = Color(0xFF2454B4) // azul-índigo do logo
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFDCE5FB)
val OnTertiaryContainerLight = Color(0xFF001E64)
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)
val BackgroundLight = Color(0xFFF7FBFD) // branco com leve tom azul
val OnBackgroundLight = Color(0xFF17242C)
val SurfaceLight = Color(0xFFF7FBFD)
val OnSurfaceLight = Color(0xFF17242C)
val SurfaceVariantLight = Color(0xFFDDE7EC)
val OnSurfaceVariantLight = Color(0xFF40484D)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF1F5F8)
val SurfaceContainerLight = Color(0xFFE9EFF3)
val SurfaceContainerHighLight = Color(0xFFE3EBEF)
val SurfaceContainerHighestLight = Color(0xFFDDE4E9)
val OutlineLight = Color(0xFF646E73)
val OutlineVariantLight = Color(0xFFC0CCD3)

// ---- Escuro (o logo vive sobre preto: tema escuro é o estado "natural" da marca) ----
val PrimaryDark = Color(0xFF4DB9E8) // azul-ciano vivo, claro o bastante sobre o fundo
val OnPrimaryDark = Color(0xFF00293A)
val PrimaryContainerDark = Color(0xFF004B68) // extremo escuro do gradiente do logo
val OnPrimaryContainerDark = Color(0xFFB3DDE4)
val SecondaryDark = Color(0xFF9CC2D6)
val OnSecondaryDark = Color(0xFF102A36)
val SecondaryContainerDark = Color(0xFF274858)
val OnSecondaryContainerDark = Color(0xFFCFE7F2)
val TertiaryDark = Color(0xFFA9BBF2) // índigo do logo clareado
val OnTertiaryDark = Color(0xFF142A66)
val TertiaryContainerDark = Color(0xFF263D77)
val OnTertiaryContainerDark = Color(0xFFDCE5FB)
val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)
val BackgroundDark = Color(0xFF0A1116) // preto azulado do fundo do logo
val OnBackgroundDark = Color(0xFFDCEFF5)
val SurfaceDark = Color(0xFF0A1116)
val OnSurfaceDark = Color(0xFFDCEFF5)
val SurfaceVariantDark = Color(0xFF27343B)
val OnSurfaceVariantDark = Color(0xFFBFC9CE)
val SurfaceContainerLowestDark = Color(0xFF05090C)
val SurfaceContainerLowDark = Color(0xFF121A1F)
val SurfaceContainerDark = Color(0xFF161F24)
val SurfaceContainerHighDark = Color(0xFF1B252A)
val SurfaceContainerHighestDark = Color(0xFF212C32)
val OutlineDark = Color(0xFF89939A)
val OutlineVariantDark = Color(0xFF3B464C)

// Semantic Tokens — variantes claro/escuro com contraste AA (>= 4.5:1)
// Claro: tons escurecidos sobre fundo #F7FBFD. Escuro: tons clareados sobre #0A1116.
// Despesas usam "−" + posição, nunca só cor (Base §7).
val IncomeLight = Color(0xFF0E6B2B)
val IncomeDark = Color(0xFF6FDB8B)
val ExpenseLight = Color(0xFFA50E0E)
val ExpenseDark = Color(0xFFFF9E99)
val PendingLight = Color(0xFF7A4A00)
val PendingDark = Color(0xFFFFC44D)
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

// Gradiente da marca, na ordem em que aparece no logo
val BrandGradientColors = listOf(Color(0xFF2454B4), Color(0xFF008FC7), Color(0xFFB3DDE4))

// Gradiente aplicado sobre superfícies (cabeçalho, card de saldo): termina no ciano
// vivo em vez do ciano pálido, e o stop ciano do meio foi calibrado para o texto branco
// passar em AA em QUALQUER ponto da faixa (pior amostra medida: 4,56:1 — com #008FC7 o
// meio ficaria em 3,65:1).
val BrandSurfaceGradientColors = listOf(Color(0xFF2454B4), Color(0xFF007EAF), Color(0xFF00658F))

// Conteúdo sobre o gradiente da marca
val OnBrandGradient = Color(0xFFFFFFFF)

// Vidro (glassmorfismo): véu claro sobre a superfície + borda translúcida
val GlassVeilLight = Color(0xB3FFFFFF)
val GlassEdgeLight = Color(0x6600A9D6)
val GlassVeilDark = Color(0x4D1B252A)
val GlassEdgeDark = Color(0x664DB9E8)
