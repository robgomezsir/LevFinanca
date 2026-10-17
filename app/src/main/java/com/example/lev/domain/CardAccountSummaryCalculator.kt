package com.example.lev.domain

import com.example.lev.data.model.Transaction
import java.time.LocalDate

object CardAccountSummaryCalculator {
    data class CardStatus(
        val limitCents: Long,
        val usedCents: Long,
        val availableCents: Long,
        val futureInstallmentsCents: Long,
        val currentInvoiceTotalCents: Long,
        val nextDueDate: LocalDate?,
    )

    fun summarize(
        cardLimitCents: Long,
        transactions: List<Transaction>,
        accountId: String,
        currentInvoiceTotalCents: Long = 0L,
        nextDueDate: LocalDate? = null,
    ): CardStatus {
        val usedCents =
            transactions
                .filter { it.accountId == accountId }
                .filter { it.type == "EXPENSE" }
                .filter { it.status == "PAID" }
                .sumOf { it.amountCents }

        val futureInstallmentsCents =
            transactions
                .filter { it.accountId == accountId }
                .filter { it.type == "EXPENSE" }
                .filter { it.installmentTotal != null && it.installmentNumber != null }
                .filter { it.installmentNumber!! < it.installmentTotal!! }
                .sumOf { it.amountCents }

        val available = (cardLimitCents - usedCents).coerceAtLeast(0L)

        return CardStatus(
            limitCents = cardLimitCents,
            usedCents = usedCents,
            availableCents = available,
            futureInstallmentsCents = futureInstallmentsCents,
            currentInvoiceTotalCents = currentInvoiceTotalCents,
            nextDueDate = nextDueDate,
        )
    }
}
