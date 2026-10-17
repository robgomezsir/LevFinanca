package com.example.lev.domain

import com.example.lev.data.model.Account
import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InvoiceGeneratorTest {
    private fun account(): Account =
        Account(
            id = "card-1",
            name = "Nubank",
            type = "CREDIT_CARD",
            initialBalanceCents = 0L,
            currency = "BRL",
            color = "#000000",
            icon = "credit_card",
            creditLimitCents = 500000L,
            closingDay = 25,
            dueDay = 5,
            closingInclusive = true,
            paymentAccountId = "checking-1",
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun transaction(
        id: String,
        amountCents: Long,
        date: LocalDate,
        installmentNumber: Int? = null,
        installmentTotal: Int? = null,
    ): Transaction =
        Transaction(
            id = id,
            type = "EXPENSE",
            description = "Compra $id",
            amountCents = amountCents,
            date = date,
            status = "PAID",
            accountId = "card-1",
            categoryId = null,
            invoiceId = null,
            installmentGroupId = if (installmentNumber != null) "group-$id" else null,
            installmentNumber = installmentNumber,
            installmentTotal = installmentTotal,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `generate invoice totals by credit card closure cycle`() {
        val account = account()
        val transactions =
            listOf(
                transaction("t1", 1000L, LocalDate.of(2026, 10, 10)),
                transaction("t2", 3000L, LocalDate.of(2026, 10, 26)),
                transaction("t3", 1200L, LocalDate.of(2026, 11, 3), installmentNumber = 2, installmentTotal = 3),
            )

        val invoices = InvoiceGenerator.generateForAccount(account, transactions)

        assertEquals(3, invoices.size)
        assertEquals(1000L, invoices.first { it.referenceMonth == LocalDate.of(2026, 10, 1) }.totalCents)
        assertEquals(3000L, invoices.first { it.referenceMonth == LocalDate.of(2026, 11, 1) }.totalCents)
        assertEquals(1200L, invoices.first { it.referenceMonth == LocalDate.of(2026, 12, 1) }.totalCents)
    }
}
