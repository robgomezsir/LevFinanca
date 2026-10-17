package com.example.lev.domain

import com.example.lev.data.model.Budget
import com.example.lev.data.model.Goal
import com.example.lev.data.model.GoalContribution
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Recurrence
import java.time.LocalDate
import java.time.YearMonth

// ---------- Faturas ----------

/**
 * "Lançamento Pagamento ID" não tem campo na entidade (o vínculo de
 * pagamento vive em Transaction.invoiceId): grava "" e ignora na leitura.
 * Totais são recalculados localmente pelo InvoiceGenerator após o pull.
 */
fun Invoice.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Cartão ID" to accountId,
        "Referência (AAAA-MM)" to YearMonth.from(referenceMonth).toSheetReference(),
        "Fechamento" to closingDate.toSheetDate(),
        "Vencimento" to dueDate.toSheetDate(),
        "Status" to (invoiceStatus.first[status] ?: status),
        "Lançamento Pagamento ID" to "",
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToInvoice(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Invoice> {
    val tab = SheetSchema.INVOICES.name
    val id = row.cell("ID")
    val cardId = row.cell("Cartão ID")
    val ref = parseSheetReference(row["Referência (AAAA-MM)"])
    val closing = parseSheetDate(row["Fechamento"])
    val due = parseSheetDate(row["Vencimento"])
    val status = invoiceStatus.second.toCode(row.cell("Status"))
    val error =
        when {
            id.isEmpty() -> "ID vazio."
            cardId.isEmpty() -> "cartão vazio."
            ref == null -> "referência inválida (use AAAA-MM)."
            closing == null -> "fechamento inválido."
            due == null -> "vencimento inválido."
            status == null -> "status desconhecido."
            else -> null
        }
    if (error != null) return err(lineNo, tab, error)
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Invoice(
            id = id,
            accountId = cardId,
            referenceMonth = checkNotNull(ref).atDay(1),
            closingDate = checkNotNull(closing),
            dueDate = checkNotNull(due),
            status = checkNotNull(status),
            totalCents = 0L,
            paidAmountCents = 0L,
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Orçamentos ----------

fun Budget.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Mês" to month,
        "Categoria ID" to categoryId,
        "Planejado (R$)" to plannedCents.toSheetMoney(),
        "Rollover" to rolloverEnabled.toSheetBool(),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToBudget(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Budget> {
    val tab = SheetSchema.BUDGETS.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    val month = row.cell("Mês")
    if (!Regex("""^\d{4}-\d{2}$""").matches(month)) return err(lineNo, tab, "mês inválido (use AAAA-MM).")
    val catId = row.cell("Categoria ID")
    if (catId.isEmpty()) return err(lineNo, tab, "categoria vazia.")
    val planned =
        parseMoneyStrict(row["Planejado (R$)"])
            ?: return err(lineNo, tab, "valor planejado inválido.")
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Budget(
            id = id,
            month = month,
            categoryId = catId,
            plannedCents = planned,
            rolloverEnabled = parseSheetBool(row["Rollover"].takeUnless { it.isNullOrBlank() } ?: "VERDADEIRO"),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Metas ----------

fun Goal.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Nome" to name,
        "Valor Alvo (R$)" to targetCents.toSheetMoney(),
        "Prazo" to (targetDate?.toSheetDate() ?: ""),
        "Conta ID" to (linkedAccountId ?: ""),
        "Status" to (goalStatus.first[status] ?: status),
        "Cor" to color,
        "Ícone" to icon,
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToGoal(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Goal> {
    val tab = SheetSchema.GOALS.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    if (row.cell("Nome").isEmpty()) return err(lineNo, tab, "nome vazio.")
    val target =
        parseMoneyStrict(row["Valor Alvo (R$)"])
            ?: return err(lineNo, tab, "valor alvo inválido.")
    val deadlineRaw = row.cell("Prazo")
    val deadline = deadlineRaw.ifEmpty { null }?.let { parseSheetDate(it) }
    if (deadlineRaw.isNotEmpty() && deadline == null) return err(lineNo, tab, "prazo inválido.")
    val status = goalStatus.second.toCode(row.cell("Status").ifEmpty { "Ativa" }) ?: "ACTIVE"
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Goal(
            id = id,
            name = row.cell("Nome"),
            targetCents = target,
            targetDate = deadline,
            linkedAccountId = row.cell("Conta ID").ifEmpty { null },
            color = row.cell("Cor").ifEmpty { "#00658F" },
            icon = row.cell("Ícone").ifEmpty { "savings" },
            status = status,
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Aportes ----------

fun GoalContribution.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Meta ID" to goalId,
        "Valor (R$)" to amountCents.toSheetMoney(),
        "Data" to date.toSheetDate(),
        "Observação" to (note ?: ""),
        "Lançamento ID" to (transactionId ?: ""),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToContribution(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<GoalContribution> {
    val tab = SheetSchema.GOAL_CONTRIBUTIONS.name
    val id = row.cell("ID")
    if (id.isEmpty()) return err(lineNo, tab, "ID vazio.")
    val goalId = row.cell("Meta ID")
    if (goalId.isEmpty()) return err(lineNo, tab, "meta vazia.")
    val amount =
        parseMoneyStrict(row["Valor (R$)"])
            ?: return err(lineNo, tab, "valor inválido.")
    val date =
        parseSheetDate(row["Data"])
            ?: return err(lineNo, tab, "data inválida.")
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        GoalContribution(
            id = id,
            goalId = goalId,
            amountCents = amount,
            date = date,
            note = row.cell("Observação").ifEmpty { null },
            transactionId = row.cell("Lançamento ID").ifEmpty { null },
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

// ---------- Recorrências ----------

fun Recurrence.toSheetRow(): SheetRow =
    mapOf(
        "ID" to id,
        "Descrição" to description,
        "Tipo" to (txTypes.first[type] ?: type),
        "Valor (R$)" to (amountCents?.toSheetMoney() ?: ""),
        "Valor Variável" to isVariable.toSheetBool(),
        "Frequência" to (frequencies.first[frequency] ?: frequency),
        "Intervalo" to interval.toString(),
        "Dia" to (dayOfMonth?.toString() ?: dayOfWeek?.toString() ?: ""),
        "Início" to startDate.toSheetDate(),
        "Fim" to (endDate?.toSheetDate() ?: ""),
        "Conta ID" to accountId,
        "Categoria ID" to (categoryId ?: ""),
        "Favorecido" to (payee ?: ""),
        "Observações" to (notes ?: ""),
        "Ativa" to active.toSheetBool(),
        "Atualizado Em" to epochToSheet(updatedAt),
        "Excluído" to deleted.toSheetBool(),
    )

fun rowToRecurrence(
    row: SheetRow,
    lineNo: Int,
): RowParseResult<Recurrence> {
    val tab = SheetSchema.RECURRENCES.name
    val id = row.cell("ID")
    val description = row.cell("Descrição")
    val type = txTypes.second.toCode(row.cell("Tipo"))
    val variable = parseSheetBool(row["Valor Variável"])
    val amountRaw = row.cell("Valor (R$)")
    val amount = parseMoneyStrict(amountRaw)
    val frequency = frequencies.second.toCode(row.cell("Frequência"))
    val start = parseSheetDate(row["Início"])
    val endRaw = row.cell("Fim")
    val end = endRaw.ifEmpty { null }?.let { parseSheetDate(it) }
    val accountId = row.cell("Conta ID")
    val error =
        recurrenceError(
            row = row,
            id = id,
            description = description,
            type = type,
            variable = variable,
            amount = amount,
            frequency = frequency,
            start = start,
            endRaw = endRaw,
            end = end,
            accountId = accountId,
        )
    if (error != null) return err(lineNo, tab, error)
    val updatedAt = sheetToEpoch(row.cell("Atualizado Em"))
    return ok(
        Recurrence(
            id = id,
            type = checkNotNull(type),
            description = description,
            amountCents = if (variable) null else checkNotNull(amount),
            isVariable = variable,
            accountId = accountId,
            categoryId = row.cell("Categoria ID").ifEmpty { null },
            payee = row.cell("Favorecido").ifEmpty { null },
            notes = row.cell("Observações").ifEmpty { null },
            frequency = checkNotNull(frequency),
            interval = row.cell("Intervalo").toIntOrNull()?.coerceAtLeast(1) ?: 1,
            dayOfMonth = row.cell("Dia").toIntOrNull(),
            startDate = checkNotNull(start),
            endDate = end,
            active = parseSheetBool(row["Ativa"].takeUnless { it.isNullOrBlank() } ?: "VERDADEIRO"),
            createdAt = updatedAt,
            updatedAt = updatedAt,
            deleted = parseSheetBool(row["Excluído"]),
        ),
    )
}

private fun recurrenceError(
    row: SheetRow,
    id: String,
    description: String,
    type: String?,
    variable: Boolean,
    amount: Long?,
    frequency: String?,
    start: LocalDate?,
    endRaw: String,
    end: LocalDate?,
    accountId: String,
): String? =
    when {
        id.isEmpty() -> "ID vazio."
        description.isEmpty() -> "descrição vazia."
        type == null -> "tipo '${row.cell("Tipo")}' desconhecido."
        !variable && amount == null -> "valor inválido."
        frequency == null -> "frequência desconhecida."
        start == null -> "início inválido."
        endRaw.isNotEmpty() && end == null -> "fim inválido."
        accountId.isEmpty() -> "conta vazia."
        else -> null
    }
