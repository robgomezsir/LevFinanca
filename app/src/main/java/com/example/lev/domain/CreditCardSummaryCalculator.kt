package com.example.lev.domain

import com.example.lev.data.model.Transaction
import kotlin.math.max

object CreditCardSummaryCalculator {
    data class Summary(
        val creditLimitCents: Long,
        val openInvoicesTotalCents: Long,
        val availableCreditCents: Long,
        val futureInstallmentsCents: Long,
    )

    fun availableCredit(
        creditLimitCents: Long,
        openInvoicesTotalCents: Long,
    ): Long = max(0L, creditLimitCents - openInvoicesTotalCents)

    fun openInvoicesTotalCents(
        transactions: List<Transaction>,
        accountId: String,
    ): Long =
        transactions
            .asSequence()
            .filter { it.accountId == accountId }
            .filter { it.type == "EXPENSE" }
            .filter { it.status == "PAID" }
            .sumOf { it.amountCents }

    fun futureInstallmentsCommitment(
        transactions: List<Transaction>,
        accountId: String,
    ): Long =
        transactions
            .asSequence()
            .filter { it.accountId == accountId }
            .filter { it.type == "EXPENSE" }
            .filter { it.installmentTotal != null && it.installmentNumber != null }
            .filter { it.installmentNumber!! < it.installmentTotal!! }
            .sumOf { it.amountCents }

    fun summarize(
        creditLimitCents: Long,
        transactions: List<Transaction>,
        accountId: String,
    ): Summary {
        val openInvoicesTotal = openInvoicesTotalCents(transactions, accountId)
        val futureInstallments = futureInstallmentsCommitment(transactions, accountId)
        return Summary(
            creditLimitCents = creditLimitCents,
            openInvoicesTotalCents = openInvoicesTotal,
            availableCreditCents = availableCredit(creditLimitCents, openInvoicesTotal),
            futureInstallmentsCents = futureInstallments,
        )
    }
}
