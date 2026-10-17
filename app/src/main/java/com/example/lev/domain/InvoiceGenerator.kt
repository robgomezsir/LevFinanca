package com.example.lev.domain

import com.example.lev.data.model.Account
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import java.time.LocalDate

object InvoiceGenerator {
    data class InvoiceDraft(
        val id: String,
        val accountId: String,
        val referenceMonth: LocalDate,
        val closingDate: LocalDate,
        val dueDate: LocalDate,
        val totalCents: Long,
        val transactionIds: List<String>,
    ) {
        fun toInvoice(createdAt: Long = System.currentTimeMillis()): Invoice =
            Invoice(
                id = id,
                accountId = accountId,
                referenceMonth = referenceMonth,
                closingDate = closingDate,
                dueDate = dueDate,
                status = "OPEN",
                totalCents = totalCents,
                paidAmountCents = 0L,
                createdAt = createdAt,
                updatedAt = createdAt,
            )
    }

    fun generateForAccount(
        account: Account,
        transactions: List<Transaction>,
    ): List<InvoiceDraft> {
        require(account.type == "CREDIT_CARD") { "Invoice generation is only supported for CREDIT_CARD accounts." }

        val grouped = linkedMapOf<LocalDate, MutableList<Transaction>>()

        for (transaction in transactions.filter { it.accountId == account.id && it.type == "EXPENSE" }) {
            val monthsAhead =
                when {
                    transaction.installmentNumber != null &&
                        transaction.installmentTotal != null ->
                        transaction.installmentNumber - 1
                    else -> 0
                }
            val invoiceRef =
                InvoiceCycle.calculateInvoiceForPurchase(
                    purchaseDate = transaction.date,
                    closingDay = account.closingDay ?: 25,
                    dueDay = account.dueDay ?: 5,
                    closingInclusive = account.closingInclusive,
                    monthsAhead = monthsAhead,
                )
            grouped.getOrPut(invoiceRef.referenceMonth) { mutableListOf() }.add(transaction)
        }

        return grouped.entries
            .sortedBy { it.key }
            .map { (referenceMonth, groupedTransactions) ->
                val invoiceRef =
                    InvoiceCycle.calculateInvoiceForPurchase(
                        purchaseDate = referenceMonth.withDayOfMonth(1),
                        closingDay = account.closingDay ?: 25,
                        dueDay = account.dueDay ?: 5,
                        closingInclusive = account.closingInclusive,
                    )

                InvoiceDraft(
                    id = "invoice-${account.id}-${referenceMonth.year}-${referenceMonth.monthValue}",
                    accountId = account.id,
                    referenceMonth = invoiceRef.referenceMonth,
                    closingDate = invoiceRef.closingDate,
                    dueDate = invoiceRef.dueDate,
                    totalCents = groupedTransactions.sumOf { it.amountCents },
                    transactionIds = groupedTransactions.map { it.id },
                )
            }
    }
}
