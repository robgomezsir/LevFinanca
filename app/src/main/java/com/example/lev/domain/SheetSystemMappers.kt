package com.example.lev.domain

import com.example.lev.data.model.MonthClosure
import com.example.lev.data.model.Rule
import com.example.lev.data.model.SystemSetting
import java.util.UUID

// ---------- Regras ----------

fun Rule.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Campo" to (matchFields.first[matchField] ?: matchField),
        "Tipo Correspondência" to (matchTypes.first[matchType] ?: matchType),
        "Padrão" to pattern,
        "Categoria ID" to (setCategoryId ?: ""),
        "Favorecido" to (setPayee ?: ""),
        "Tags" to (setTags ?: ""),
        "Prioridade" to priority.toString(),
        "Ativa" to active.toSheetBool(),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToRule(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Rule> {
    val tab = SheetSchema.RULES.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    val field =
        matchFields.second.toCode(row.cell("Campo"))
            ?: return err(lineNo, tab, "campo desconhecido.")
    val matchType =
        matchTypes.second.toCode(row.cell("Tipo Correspondência"))
            ?: return err(lineNo, tab, "tipo de correspondência desconhecido.")
    if (row.cell("Padrão").isEmpty()) return err(lineNo, tab, "padrão vazio.")
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Rule(
            id = id,
            matchField = field,
            matchType = matchType,
            pattern = row.cell("Padrão"),
            setCategoryId = row.cell("Categoria ID").ifEmpty { null },
            setPayee = row.cell("Favorecido").ifEmpty { null },
            setTags = row.cell("Tags").ifEmpty { null },
            priority = row.cell("Prioridade").toIntOrNull() ?: 0,
            active = parseSheetBool(row["Ativa"].takeUnless { it.isNullOrBlank() } ?: "VERDADEIRO"),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Arquivo ----------

fun MonthClosure.toSheetRow(): SheetRow =
    mapOf(
        "Mês" to month,
        "Receitas (R$)" to incomeCents.toSheetMoney(),
        "Despesas (R$)" to expenseCents.toSheetMoney(),
        "Saldo (R$)" to balanceCents.toSheetMoney(),
        "Fechado Em" to epochToSheet(closedAt),
        "Observação" to (note ?: ""),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToClosure(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<MonthClosure> {
    val tab = SheetSchema.ARCHIVE.name
    val month = row.cell("Mês")
    val income = parseMoneyStrict(row["Receitas (R$)"])
    val expense = parseMoneyStrict(row["Despesas (R$)"])
    val balance = parseMoneyStrict(row["Saldo (R$)"])
    val closedAt = sheetToEpoch(row.cell("Fechado Em")).takeIf { it > 0L }
    val error =
        when {
            !Regex("""^\d{4}-\d{2}$""").matches(month) -> "mês inválido (use AAAA-MM)."
            income == null -> "receitas inválidas."
            expense == null -> "despesas inválidas."
            balance == null -> "saldo inválido."
            closedAt == null -> "data de fechamento inválida."
            else -> null
        }
    if (error != null) return err(lineNo, tab, error)
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        MonthClosure(
            id = month,
            month = month,
            incomeCents = checkNotNull(income),
            expenseCents = checkNotNull(expense),
            balanceCents = checkNotNull(balance),
            closedAt = checkNotNull(closedAt),
            note = row.cell("Observação").ifEmpty { null },
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Config ----------

fun SystemSetting.toSheetRow(): SheetRow =
    mapOf(
        "Chave" to key,
        "Valor" to value,
        "Atualizado Em" to epochToSheet(updatedAt),
    )

fun rowToSetting(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<SystemSetting> {
    val key = row.cell("Chave")
    if (key.isEmpty()) return err(lineNo, SheetSchema.CONFIG.name, "chave vazia.")
    return ok(
        SystemSetting(
            key = key,
            value = row.cell("Valor"),
            updatedAt = sheetToEpoch(row.cell("Atualizado Em")),
        ),
    )
}

/** ID estável para divisões (a aba Divisoes não tem coluna ID no M7). */
fun splitIdOf(
    transactionId: String,
    categoryId: String,
    amountCents: Long,
    note: String,
): String {
    val namespace = "$transactionId|$categoryId|$amountCents|$note"
    return UUID.nameUUIDFromBytes(namespace.toByteArray(Charsets.UTF_8)).toString()
}
