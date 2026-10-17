package com.example.lev.domain

object MoneyParser {
    /**
     * Parses a Brazilian currency string into cents (Long).
     * Accepts formats like: "R$ 1.234,56", "1234,56", "1234.56", "1.234", or empty.
     */
    fun parse(input: String?): Long {
        if (input.isNullOrBlank()) return 0L

        val cleanString = input.replace(Regex("[^0-9.,\\-+]"), "")
        if (cleanString.isBlank() || cleanString == "-" || cleanString == "+") return 0L

        // Evaluate basic expression (e.g. 12,50+8)
        val tokens = cleanString.split(Regex("(?=[-+])|(?<=[-+])"))
        var totalCents = 0L
        var currentOperator = "+"

        for (token in tokens) {
            val t = token.trim()
            if (t.isEmpty()) continue
            if (t == "+" || t == "-") {
                currentOperator = t
                continue
            }

            val cents = parseSingleToken(t)
            if (currentOperator == "+") {
                totalCents += cents
            } else if (currentOperator == "-") {
                totalCents -= cents
            }
        }
        return totalCents
    }

    private fun parseSingleToken(token: String): Long {
        var cleanToken = token.replace(Regex("[^0-9.,]"), "")
        if (cleanToken.isEmpty()) return 0L

        var cents: Long = 0
        if (cleanToken.contains(",")) {
            val parts = cleanToken.split(",")
            val integerPart = parts[0].replace(".", "").toLongOrNull() ?: 0L
            val fractionPart = parts[1].padEnd(2, '0').substring(0, 2).toLongOrNull() ?: 0L
            cents = (integerPart * 100) + fractionPart
        } else if (cleanToken.contains(".")) {
            val parts = cleanToken.split(".")
            if (parts.last().length == 2 && parts.size == 2) {
                val integerPart = parts[0].toLongOrNull() ?: 0L
                val fractionPart = parts[1].toLongOrNull() ?: 0L
                cents = (integerPart * 100) + fractionPart
            } else {
                val integerPart = cleanToken.replace(".", "").toLongOrNull() ?: 0L
                cents = integerPart * 100
            }
        } else {
            cents = (cleanToken.toLongOrNull() ?: 0L) * 100
        }
        return cents
    }
}
