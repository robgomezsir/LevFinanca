package com.example.lev.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object MoneyFormatter {
    private val localeBR = Locale("pt", "BR")
    private val numberFormat =
        NumberFormat.getCurrencyInstance(localeBR).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }

    /**
     * Formats cents (Long) into a Brazilian currency string (e.g., "R$ 1.234,56").
     */
    fun format(cents: Long): String {
        val absValue = abs(cents)
        val formatted =
            numberFormat.format(absValue / 100.0)
                .replace('\u00A0', ' ')
                .replace("R$", "R$ ")
                .replace("  ", " ")
                .trim()

        return if (cents < 0) "-$formatted" else formatted
    }

    /**
     * Formats without the currency symbol (e.g., "1.234,56").
     */
    fun formatNumberOnly(cents: Long): String {
        return format(cents)
            .replace("R$", "")
            .replace("-", "")
            .replace(" ", "")
            .trim()
    }
}
