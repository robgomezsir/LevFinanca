package com.example.lev.domain

object MoneyWordsFormatter {
    private val units =
        listOf(
            "zero", "um", "dois", "três", "quatro", "cinco", "seis", "sete", "oito", "nove",
        )
    private val teens =
        listOf(
            "dez", "onze", "doze", "treze", "quatorze", "quinze", "dezesseis", "dezessete", "dezoito", "dezenove",
        )
    private val tens =
        listOf(
            "vinte",
            "trinta",
            "quarenta",
            "cinquenta",
            "sessenta",
            "setenta",
            "oitenta",
            "noventa",
        )

    fun toWords(cents: Long): String {
        val reais = cents / 100L
        val centavos = (cents % 100L).toInt()

        val reaisText =
            when {
                reais == 0L -> "zero"
                else -> convertNumber(reais)
            }
        val centsText = if (centavos == 0) "" else " e ${convertNumber(centavos.toLong())} centavos"

        return "$reaisText${if (reais == 1L) " real" else " reais"}$centsText"
            .replace("  ", " ")
            .trim()
    }

    private fun convertNumber(value: Long): String =
        when {
            value < 10 -> units[value.toInt()]
            value < 20 -> teens[value.toInt() - 10]
            value < 100 -> convertTens(value)
            value < 1000 -> convertHundreds(value)
            else -> convertLarge(value)
        }

    private fun convertTens(value: Long): String {
        val tensValue = (value / 10).toInt()
        val onesValue = (value % 10).toInt()
        val suffix = if (onesValue == 0) "" else " e ${units[onesValue]}"
        return "${tens[tensValue - 2]}$suffix"
    }

    private fun convertHundreds(value: Long): String {
        val hundred = value / 100
        val remainder = value % 100
        val prefix =
            when (hundred.toInt()) {
                0 -> ""
                1 -> "cem"
                2 -> "duzentos"
                3 -> "trezentos"
                4 -> "quatrocentos"
                5 -> "quinhentos"
                6 -> "seiscentos"
                7 -> "setecentos"
                8 -> "oitocentos"
                else -> "novecentos"
            }
        val suffix = if (remainder == 0L) "" else " e ${convertNumber(remainder)}"
        return "$prefix$suffix".trim()
    }

    private fun convertLarge(value: Long): String {
        val scale =
            when {
                value < 1_000_000L -> 1000L to "mil"
                value < 1_000_000_000L -> 1_000_000L to "milhão"
                else -> 1_000_000_000L to "bilhão"
            }

        val divisor = scale.first
        val label = scale.second
        val quotient = value / divisor
        val remainder = value % divisor
        val prefix =
            if (quotient == 1L && label == "mil") {
                "mil"
            } else if (quotient == 1L) {
                "um $label"
            } else {
                "${convertNumber(quotient)} $label"
            }
        val suffix = if (remainder == 0L) "" else " ${convertNumber(remainder)}"
        return "$prefix$suffix".trim()
    }
}

data class ReleaseAuditReport(
    val hasIssues: Boolean,
    val issues: List<String>,
)

object ReleaseAudit {
    fun audit(lines: List<String>): ReleaseAuditReport {
        val issues = linkedSetOf<String>()
        lines.forEach { line ->
            if (line.contains("!!")) {
                issues += "unsafe-non-null-assertion"
            }
            if (line.contains("Log.") || line.contains("android.util.Log")) {
                issues += "sensitive-logging"
            }
        }
        return ReleaseAuditReport(hasIssues = issues.isNotEmpty(), issues = issues.toList())
    }
}

data class PerformanceBudget(
    val launchMs: Double,
    val listRenderMs: Double,
    val maxLaunchMs: Double = 2000.0,
    val maxListRenderMs: Double = 300.0,
) {
    fun withinBudget(): Boolean = launchMs <= maxLaunchMs && listRenderMs <= maxListRenderMs
}
