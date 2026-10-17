package com.example.lev.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs

internal fun SheetRow.cell(header: String): String = (this[header] ?: "").trim()

internal val sheetDateFmt: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun LocalDate.toSheetDate(): String = format(sheetDateFmt)

fun YearMonth.toSheetReference(): String = "%04d-%02d".format(year, monthValue)

/** Long em centavos → decimal com ponto ("1500.50"), formato da planilha. */
fun Long.toSheetMoney(): String {
    val sign = if (this < 0L) "-" else ""
    val v = abs(this)
    return "$sign${v / 100}.${(v % 100).toString().padStart(2, '0')}"
}

fun Boolean.toSheetBool(): String = if (this) "VERDADEIRO" else "FALSO"

fun epochToSheet(value: Long): String = if (value <= 0L) "" else value.toString()

fun sheetToEpoch(raw: String): Long = raw.filter { it.isDigit() }.toLongOrNull() ?: 0L

/**
 *Parsing estrito de dinheiro: null quando vazio ou inválido (o MoneyParser
 * normal devolve 0 para lixo, o que apagaria a distinção para o log de erros).
 */
fun parseMoneyStrict(raw: String?): Long? {
    val s = (raw ?: "").trim()
    if (s.isEmpty()) return null
    val cents = MoneyParser.parse(s)
    if (cents == 0L) {
        val digits = s.filter { it.isDigit() }
        if (digits.isEmpty() || digits.trimStart('0').isNotEmpty()) return null
    }
    return cents
}

/** dd/MM/yyyy, ISO (AAAA-MM-DD) ou dd/MM (assume o ano atual). */
fun parseSheetDate(raw: String?): LocalDate? {
    val s = (raw ?: "").trim()
    if (s.isEmpty()) return null
    runCatching { return LocalDate.parse(s, sheetDateFmt) }
    runCatching { return LocalDate.parse(s) }
    val short = Regex("""^(\d{1,2})/(\d{1,2})$""").matchEntire(s)
    if (short != null) {
        val (d, m) = short.destructured
        runCatching { return LocalDate.of(LocalDate.now().year, m.toInt(), d.toInt()) }
    }
    return null
}

fun parseSheetBool(raw: String?): Boolean {
    return when ((raw ?: "").trim().uppercase()) {
        "VERDADEIRO", "TRUE", "1", "SIM", "S", "X" -> true
        else -> false
    }
}

fun parseSheetReference(raw: String?): YearMonth? {
    val s = (raw ?: "").trim()
    if (s.isEmpty()) return null
    runCatching {
        val parts = s.split("-")
        if (parts.size == 2) return YearMonth.of(parts[0].toInt(), parts[1].toInt())
    }
    runCatching { return YearMonth.parse(s) }
    return null
}

internal fun <T> err(
    lineNo: Int,
    tab: String,
    reason: String,
): RowParseResult<T> = RowParseResult(null, "linha $lineNo ($tab): $reason")

/** Rótulos pt-BR ↔ códigos, leitura tolerante (aceita código ou rótulo, sem caixa). */
internal fun labelMap(vararg pairs: Pair<String, String>): Pair<Map<String, String>, Map<String, String>> {
    val toLabel = pairs.toMap()
    val fromLabel =
        pairs.flatMap { (code, label) -> listOf(code to code, label to code) }
            .associate { (k, v) -> k.uppercase() to v }
    return toLabel to fromLabel
}

internal val accountTypes =
    labelMap(
        "CHECKING" to "Conta corrente",
        "SAVINGS" to "Poupança",
        "CASH" to "Dinheiro",
        "CREDIT_CARD" to "Cartão de crédito",
        "INVESTMENT" to "Investimento",
        "BENEFIT" to "Benefício",
        "LOAN" to "Empréstimo",
    )
internal val txTypes =
    labelMap(
        "INCOME" to "Receita",
        "EXPENSE" to "Despesa",
        "TRANSFER" to "Transferência",
        "ADJUSTMENT" to "Ajuste",
    )
internal val txStatus = labelMap("PAID" to "Pago", "PENDING" to "Pendente")
internal val categoryKinds = labelMap("INCOME" to "Receita", "EXPENSE" to "Despesa")
internal val invoiceStatus =
    labelMap(
        "OPEN" to "Aberta",
        "CLOSED" to "Fechada",
        "PAID" to "Paga",
    )
internal val goalStatus =
    labelMap(
        "ACTIVE" to "Ativa",
        "ACHIEVED" to "Concluída",
        "ARCHIVED" to "Arquivada",
    )
internal val frequencies =
    labelMap(
        "DAILY" to "Diária",
        "WEEKLY" to "Semanal",
        "MONTHLY" to "Mensal",
        "YEARLY" to "Anual",
    )
internal val matchFields = labelMap("DESCRIPTION" to "Descrição", "PAYEE" to "Favorecido")
internal val matchTypes =
    labelMap(
        "CONTAINS" to "Contém",
        "STARTS_WITH" to "Começa com",
        "REGEX" to "Regex",
    )

internal fun Map<String, String>.toCode(raw: String): String? = this[raw.trim().uppercase()]
