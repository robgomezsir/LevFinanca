package com.example.lev.domain

import com.example.lev.data.model.Account
import com.example.lev.data.model.Category

/** Linha da planilha: valores sempre texto, mapeados pelo NOME do cabeçalho. */
typealias SheetRow = Map<String, String?>

data class RowParseResult<T>(val value: T?, val error: String?)

internal fun <T> ok(value: T): RowParseResult<T> = RowParseResult(value, null)

/** Elvis fora da contagem de complexidade dos mapeadores. */
internal fun str(value: String?): String = value ?: ""

// ---------- Contas ----------

fun Account.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Nome" to name,
        "Tipo" to (accountTypes.first[type] ?: type),
        "Saldo Inicial (R$)" to initialBalanceCents.toSheetMoney(),
        "Moeda" to currency,
        "Cor" to color,
        "Ícone" to icon,
        "Limite (R$)" to (creditLimitCents?.toSheetMoney() ?: ""),
        "Dia Fechamento" to (closingDay?.toString() ?: ""),
        "Dia Vencimento" to (dueDay?.toString() ?: ""),
        "Conta Pagamento ID" to (paymentAccountId ?: ""),
        "Na Soma" to includeInTotals.toSheetBool(),
        "Arquivada" to archived.toSheetBool(),
        "Ordem" to sortOrder.toString(),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToAccount(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Account> {
    val tab = SheetSchema.ACCOUNTS.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    val balance =
        parseMoneyStrict(row["Saldo Inicial (R$)"])
            ?: return err(lineNo, tab, "saldo inicial inválido.")
    val type =
        accountTypes.second.toCode(row.cell("Tipo"))
            ?: return err(lineNo, tab, "tipo '${row.cell("Tipo")}' desconhecido.")
    return ok(
        Account(
            id = id,
            name = row.cell("Nome").ifEmpty { id },
            type = type,
            initialBalanceCents = balance,
            currency = row.cell("Moeda").ifEmpty { "BRL" },
            color = row.cell("Cor").ifEmpty { "#00658F" },
            icon = row.cell("Ícone").ifEmpty { "account_balance_wallet" },
            includeInTotals = parseSheetBool(row["Na Soma"].takeUnless { it.isNullOrBlank() } ?: "VERDADEIRO"),
            archived = parseSheetBool(row["Arquivada"]),
            sortOrder = row.cell("Ordem").toIntOrNull() ?: 0,
            creditLimitCents = parseMoneyStrict(row["Limite (R$)"].takeUnless { it.isNullOrBlank() }),
            closingDay = row.cell("Dia Fechamento").toIntOrNull(),
            dueDay = row.cell("Dia Vencimento").toIntOrNull(),
            paymentAccountId = row.cell("Conta Pagamento ID").ifEmpty { null },
            createdAt = sheetToEpoch(row.cell("Atualizado Em")),
            updatedAt = sheetToEpoch(row.cell("Atualizado Em")),
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Categorias ----------

fun Category.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Nome" to name,
        "Tipo" to (categoryKinds.first[kind] ?: kind),
        "Categoria Pai ID" to (parentId ?: ""),
        "Cor" to color,
        "Ícone" to icon,
        "Arquivada" to archived.toSheetBool(),
        "Ordem" to sortOrder.toString(),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToCategory(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Category> {
    val tab = SheetSchema.CATEGORIES.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    val kind =
        categoryKinds.second.toCode(row.cell("Tipo"))
            ?: return err(lineNo, tab, "tipo '${row.cell("Tipo")}' desconhecido.")
    return ok(
        Category(
            id = id,
            name = row.cell("Nome").ifEmpty { id },
            kind = kind,
            parentId = row.cell("Categoria Pai ID").ifEmpty { null },
            color = row.cell("Cor").ifEmpty { "#00658F" },
            icon = row.cell("Ícone").ifEmpty { "category" },
            archived = parseSheetBool(row["Arquivada"]),
            sortOrder = row.cell("Ordem").toIntOrNull() ?: 0,
            createdAt = sheetToEpoch(row.cell("Atualizado Em")),
            updatedAt = sheetToEpoch(row.cell("Atualizado Em")),
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}
