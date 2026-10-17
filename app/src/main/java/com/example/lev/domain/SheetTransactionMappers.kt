package com.example.lev.domain

import com.example.lev.data.model.Transaction
import com.example.lev.data.model.TransactionSplit

// ---------- Lançamentos ----------

/**
 * As colunas "Conta" e "Categoria" são só para leitura humana: o app grava
 * (via [accountNameOf]/[categoryNameOf]) mas ignora na leitura, usando os IDs.
 * Tags não têm tabela local na v1 (M7 prevê CSV): grava "" e ignora na leitura.
 */
fun Transaction.toSheetRow(
    accountNameOf: (String) -> String = { "" },
    categoryNameOf: (String?) -> String = { "" },
): SheetRow =
    mapOf(
        "ID" to id,
        "Tipo" to (txTypes.first[type] ?: type),
        "Descrição" to description,
        "Valor (R$)" to amountCents.toSheetMoney(),
        "Data" to date.toSheetDate(),
        "Vencimento" to str(dueDate?.toSheetDate()),
        "Pago Em" to str(paidDate?.toSheetDate()),
        "Status" to (txStatus.first[status] ?: status),
        "Conta ID" to accountId,
        "Conta" to accountNameOf(accountId),
        "Conta Destino ID" to str(destinationAccountId),
        "Categoria ID" to str(categoryId),
        "Categoria" to categoryNameOf(categoryId),
        "Favorecido" to str(payee),
        "Tags" to "",
        "Observações" to str(notes),
        "Parcelamento ID" to str(installmentGroupId),
        "Parcela" to str(installmentNumber?.toString()),
        "Total Parcelas" to str(installmentTotal?.toString()),
        "Recorrência ID" to str(recurrenceId),
        "Fatura ID" to str(invoiceId),
        "Meta ID" to str(goalId),
        "Conciliado" to reconciled.toSheetBool(),
        "Criado Em" to epochToSheet(createdAt),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToTransaction(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Transaction> {
    val tab = SheetSchema.TRANSACTIONS.name
    val id = row.cell("ID")
    val type = txTypes.second.toCode(row.cell("Tipo"))
    val amount = parseMoneyStrict(row["Valor (R$)"])
    val date = parseSheetDate(row["Data"])
    val status = txStatus.second.toCode(row.cell("Status"))
    val accountId = row.cell("Conta ID")
    val error =
        when {
            id.isEmpty() -> "ID vazio."
            type == null -> "tipo '${row.cell("Tipo")}' desconhecido."
            amount == null -> "valor '${row.cell("Valor (R$)")}' inválido."
            date == null -> "data '${row.cell("Data")}' inválida."
            status == null -> "status '${row.cell("Status")}' desconhecido."
            accountId.isEmpty() -> "conta vazia."
            else -> null
        }
    if (error != null) return err(lineNo, tab, error)
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Transaction(
            id = id,
            type = checkNotNull(type),
            description = row.cell("Descrição").ifEmpty { "(sem descrição)" },
            amountCents = checkNotNull(amount),
            date = checkNotNull(date),
            dueDate = parseSheetDate(row["Vencimento"]),
            paidDate = parseSheetDate(row["Pago Em"]),
            status = checkNotNull(status),
            accountId = accountId,
            destinationAccountId = row.cell("Conta Destino ID").ifEmpty { null },
            categoryId = row.cell("Categoria ID").ifEmpty { null },
            payee = row.cell("Favorecido").ifEmpty { null },
            notes = row.cell("Observações").ifEmpty { null },
            installmentGroupId = row.cell("Parcelamento ID").ifEmpty { null },
            installmentNumber = row.cell("Parcela").toIntOrNull(),
            installmentTotal = row.cell("Total Parcelas").toIntOrNull(),
            recurrenceId = row.cell("Recorrência ID").ifEmpty { null },
            invoiceId = row.cell("Fatura ID").ifEmpty { null },
            goalId = row.cell("Meta ID").ifEmpty { null },
            reconciled = parseSheetBool(row["Conciliado"]),
            createdAt = sheetToEpoch(row.cell("Criado Em")).takeIf { it > 0L } ?: updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Divisões (sem coluna ID no M7: identidade no repositório) ----------

fun TransactionSplit.toSheetRow(): SheetRow =
    mapOf(
        "Lançamento ID" to transactionId,
        "Categoria ID" to categoryId,
        "Valor (R$)" to amountCents.toSheetMoney(),
        "Observação" to (note ?: ""),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToSplit(
    row: SheetRow,
    lineNo: Int,
    idOf: (transactionId: String, categoryId: String, amountCents: Long, note: String) -> String,
): RowParseResult<TransactionSplit> {
    val tab = SheetSchema.SPLITS.name
    val txId = row.cell("Lançamento ID")
    if (txId.isEmpty()) return err(lineNo, tab, "lançamento vazio.")
    val catId = row.cell("Categoria ID")
    if (catId.isEmpty()) return err(lineNo, tab, "categoria vazia.")
    val amount =
        parseMoneyStrict(row["Valor (R$)"])
            ?: return err(lineNo, tab, "valor inválido.")
    val note = row.cell("Observação")
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        TransactionSplit(
            id = idOf(txId, catId, amount, note),
            transactionId = txId,
            categoryId = catId,
            amountCents = amount,
            note = note.ifEmpty { null },
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}
