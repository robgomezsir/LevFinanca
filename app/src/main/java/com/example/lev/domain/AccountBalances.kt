package com.example.lev.domain

import com.example.lev.data.model.Account
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import java.time.LocalDate
import kotlin.math.max

/**
 * Dinâmica única de saldos para todos os cards e páginas (Base §5.3/§5.5):
 * Painel, Contas e Relatórios/Patrimônio usam exatamente estas funções, então
 * os mesmos lançamentos geram os mesmos números em todo o app.
 */
object AccountBalances {
    private val assetTypes =
        setOf("CHECKING", "SAVINGS", "CASH", "INVESTMENT", "BENEFIT")

    /** Saldo real de conta não-cartão; cartão não é dinheiro em caixa. */
    fun currentBalance(
        account: Account,
        transactions: List<Transaction>,
    ): Long {
        if (account.type == "CREDIT_CARD") return 0L
        return BalanceCalculator.calculateCurrentBalance(
            initialBalance = account.initialBalanceCents,
            transactions = transactions,
            accountId = account.id,
        )
    }

    /** Meses de referência com fatura paga (por conta). */
    fun paidMonths(
        accountId: String,
        invoices: List<Invoice>,
    ): Set<LocalDate> =
        invoices
            .filter { it.accountId == accountId && it.status == "PAID" }
            .map { it.referenceMonth }
            .toSet()

    /** Rascunhos de fatura em aberto (ciclo corrente em diante, menos pagas). */
    fun openDrafts(
        account: Account,
        transactions: List<Transaction>,
        invoices: List<Invoice>,
    ): List<InvoiceGenerator.InvoiceDraft> {
        if (account.type != "CREDIT_CARD") return emptyList()
        val paid = paidMonths(account.id, invoices)
        return InvoiceGenerator.generateForAccount(account, transactions)
            .filter { it.referenceMonth !in paid }
    }

    fun currentCycleReference(
        account: Account,
        today: LocalDate = LocalDate.now(),
    ): LocalDate =
        InvoiceCycle.calculateInvoiceForPurchase(
            purchaseDate = today,
            closingDay = account.closingDay ?: 25,
            dueDay = account.dueDay ?: 5,
            closingInclusive = account.closingInclusive,
        ).referenceMonth

    /** Total em aberto do cartão (ciclos não pagos). */
    fun cardOpenTotal(
        account: Account,
        transactions: List<Transaction>,
        invoices: List<Invoice>,
    ): Long = openDrafts(account, transactions, invoices).sumOf { it.totalCents }

    /** Parcelas vincendas: ciclos em aberto após o corrente. */
    fun cardFutureTotal(
        account: Account,
        transactions: List<Transaction>,
        invoices: List<Invoice>,
        today: LocalDate = LocalDate.now(),
    ): Long {
        val current = currentCycleReference(account, today)
        return openDrafts(account, transactions, invoices)
            .filter { it.referenceMonth.isAfter(current) }
            .sumOf { it.totalCents }
    }

    data class NetWorth(
        val assetsCents: Long,
        val liabilitiesCents: Long,
    ) {
        val netCents: Long get() = assetsCents - liabilitiesCents
    }

    /**
     * Patrimônio: ativos = saldos reais das contas que entram na soma;
     * passivos = faturas em aberto dos cartões + dívidas de empréstimos
     * (saldo devedor, nunca negativo).
     */
    fun netWorth(
        accounts: List<Account>,
        transactions: List<Transaction>,
        invoices: List<Invoice>,
    ): NetWorth {
        var assets = 0L
        var liabilities = 0L
        for (account in accounts) {
            if (!account.includeInTotals) continue
            when (account.type) {
                in assetTypes ->
                    assets += currentBalance(account, transactions)
                "CREDIT_CARD" ->
                    liabilities += cardOpenTotal(account, transactions, invoices)
                "LOAN" ->
                    liabilities += max(0L, currentBalance(account, transactions))
            }
        }
        return NetWorth(assets, liabilities)
    }
}
