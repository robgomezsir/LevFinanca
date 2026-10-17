package com.example.lev.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.lev.R

/**
 * Fonte da marca (SIL OFL 1.1, licença embutida em `res/raw/poiret_one_ofl.txt`).
 *
 * Tem um único peso e traço fino: ficou restrita ao wordmark ("leve") e ao
 * título de tela (`headlineLarge`) — nunca em valores monetários, section
 * headers, tabelas ou qualquer texto funcional, onde a legibilidade e o
 * alinhamento tabular (`tnum`) importam mais que a identidade.
 */
val PoiretOne =
    FontFamily(
        Font(R.font.poiret_one_regular, FontWeight.Normal),
    )

// Tabular numbers (tnum) to keep money values aligned
val tabularTextStyle =
    TextStyle(
        fontFamily = FontFamily.Default,
        fontFeatureSettings = "tnum",
    )

val Typography =
    Typography(
        displayLarge =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp,
            ),
        displayMedium =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp,
            ),
        displaySmall =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                lineHeight = 44.sp,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = PoiretOne,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = 0.sp,
            ),
        headlineMedium =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                letterSpacing = 0.sp,
            ),
        titleMedium =
            TextStyle(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                letterSpacing = 0.1.sp,
            ),
        titleSmall =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        // Valores monetários: sempre tabulares, herdado de tabularTextStyle.
        bodyLarge =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp,
            ),
        bodyMedium =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.25.sp,
            ),
        bodySmall =
            tabularTextStyle.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.4.sp,
            ),
        labelLarge =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.3.sp,
            ),
        labelSmall =
            TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp,
            ),
    )
