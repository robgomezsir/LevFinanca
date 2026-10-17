package com.example.lev.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Raios da escala aprovada (lote 1): menor raio é mais discreto, sem variações
// soltas fora da escala. Cards usam sempre shapes.medium ou shapes.large.
val Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(14.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp),
    )
