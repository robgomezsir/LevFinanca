package com.example.lev.domain

import com.example.lev.data.model.Transaction
import java.time.LocalDate

object BalanceCalculator {
    fun calculateCurrentBalance(
        initialBalance: Long,
        transactions: List<Transaction>,
        accountId: String,
        accountType: String? = null,
    ): Long {
        val ignoreForCash = accountType == "CREDIT_CARD"
        var balance = initialBalance
        for (tx in transactions) {
            if (tx.status != "PAID") continue
            balance += paidDelta(tx, accountId, ignoreForCash)
        }
        return balance
    }

    fun calculatePredictedBalance(
        currentBalance: Long,
        pendingTransactions: List<Transaction>,
        accountId: String,
        targetDate: LocalDate,
        accountType: String? = null,
    ): Long {
        val ignoreForCash = accountType == "CREDIT_CARD"
        var balance = currentBalance
        for (tx in pendingTransactions) {
            if (tx.status != "PENDING") continue
            val dateToCheck = tx.dueDate ?: tx.date
            if (dateToCheck.isAfter(targetDate)) continue
            balance += pendingDelta(tx, accountId, ignoreForCash)
        }
        return balance
    }

    private fun paidDelta(
        tx: Transaction,
        accountId: String,
        ignoreForCash: Boolean,
    ): Long {
        val matchesAccount = tx.accountId == accountId && !ignoreForCash
        return when (tx.type) {
            "INCOME" -> if (matchesAccount) tx.amountCents else 0L
            "EXPENSE" -> if (matchesAccount) -tx.amountCents else 0L
            "ADJUSTMENT" -> if (tx.accountId == accountId) tx.amountCents else 0L
            "TRANSFER" -> transferDelta(tx, accountId, ignoreForCash)
            else -> 0L
        }
    }

    private fun pendingDelta(
        tx: Transaction,
        accountId: String,
        ignoreForCash: Boolean,
    ): Long {
        val matchesAccount = tx.accountId == accountId && !ignoreForCash
        return when (tx.type) {
            "INCOME" -> if (matchesAccount) tx.amountCents else 0L
            "EXPENSE" -> if (matchesAccount) -tx.amountCents else 0L
            "TRANSFER" -> transferDelta(tx, accountId, ignoreForCash)
            else -> 0L
        }
    }

    private fun transferDelta(
        tx: Transaction,
        accountId: String,
        ignoreForCash: Boolean,
    ): Long {
        var delta = 0L
        if (tx.accountId == accountId && !ignoreForCash) delta -= tx.amountCents
        if (tx.destinationAccountId == accountId) delta += tx.amountCents
        return delta
    }
}
