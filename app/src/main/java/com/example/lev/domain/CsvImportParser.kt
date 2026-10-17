package com.example.lev.domain

data class ImportRow(
    val id: String,
    val description: String,
    val amountText: String,
    val dateText: String,
    val type: String,
)

object CsvImportParser {
    fun normalize(rows: List<ImportRow>): List<ImportRow> {
        val unique = linkedMapOf<String, ImportRow>()
        rows.forEach { row ->
            val key = duplicateKey(row.description, row.amountText, row.dateText)
            if (key !in unique) {
                unique[key] = row
            }
        }
        return unique.values.toList()
    }

    fun duplicateKey(
        description: String,
        amountText: String,
        dateText: String,
    ): String {
        val normalizedDescription = description.trim().lowercase()
        val normalizedAmount = MoneyParser.parse(amountText)
        val normalizedDate =
            dateText.trim().let {
                val parsed =
                    if (it.contains('/')) {
                        val parts = it.split('/')
                        if (parts.size == 3) {
                            "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}"
                        } else {
                            it
                        }
                    } else {
                        it
                    }
                parsed
            }
        return "$normalizedDescription|$normalizedAmount|$normalizedDate"
    }
}
