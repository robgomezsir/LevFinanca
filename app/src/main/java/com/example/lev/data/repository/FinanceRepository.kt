package com.example.lev.data.repository

import com.example.lev.data.local.AccountDao
import com.example.lev.data.local.BudgetDao
import com.example.lev.data.local.CategoryDao
import com.example.lev.data.local.GoalContributionDao
import com.example.lev.data.local.GoalDao
import com.example.lev.data.local.InvoiceDao
import com.example.lev.data.local.RecurrenceDao
import com.example.lev.data.local.TransactionDao
import com.example.lev.data.model.Account
import com.example.lev.data.model.Budget
import com.example.lev.data.model.Category
import com.example.lev.data.model.Goal
import com.example.lev.data.model.GoalContribution
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Recurrence
import com.example.lev.data.model.Transaction
import com.example.lev.domain.InstallmentSplitter
import com.example.lev.domain.InvoiceGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.util.UUID

class FinanceRepository(
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val invoiceDao: InvoiceDao? = null,
    private val budgetDao: BudgetDao? = null,
    private val goalDao: GoalDao? = null,
    private val goalContributionDao: GoalContributionDao? = null,
    private val recurrenceDao: RecurrenceDao? = null,
    private val onLocalChange: () -> Unit = {},
) {
    fun getAccounts(): Flow<List<Account>> = accountDao.getAllActive()

    suspend fun saveAccount(account: Account) {
        accountDao.insert(account)
        onLocalChange()
    }

    suspend fun updateAccount(account: Account) {
        accountDao.update(account)
        onLocalChange()
    }

    suspend fun deleteAccount(id: String) {
        accountDao.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }

    fun getCategories(): Flow<List<Category>> = categoryDao.getAllActive()

    suspend fun saveCategory(category: Category) {
        categoryDao.insert(category)
        onLocalChange()
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
        onLocalChange()
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }

    fun getTransactions(): Flow<List<Transaction>> = transactionDao.getAllActive()

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.update(transaction)
        onLocalChange()
    }

    suspend fun deleteTransaction(id: String) {
        transactionDao.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }

    suspend fun saveTransaction(transaction: Transaction) {
        transactionDao.insert(transaction)
        onLocalChange()

        val accounts = accountDao.getAllActive().first()
        val account = accounts.firstOrNull { it.id == transaction.accountId && it.type == "CREDIT_CARD" }
        if (account == null) return

        val allTransactions = transactionDao.getAllActive().first()
        val generatedInvoices = syncInvoicesForAccount(account, allTransactions)
        generatedInvoices.forEach { saveGeneratedInvoice(it) }
    }

    fun getInvoicesForAccount(accountId: String): Flow<List<Invoice>> =
        invoiceDao?.getInvoicesForAccount(accountId) ?: kotlinx.coroutines.flow.emptyFlow()

    fun getAllInvoices(): Flow<List<Invoice>> = invoiceDao?.getAll() ?: kotlinx.coroutines.flow.emptyFlow()

    /**
     * Persiste fatura gerada a partir dos lançamentos sem ressuscitar fatura
     * paga: se a linha armazenada está PAID, mantém status e valor pago e só
     * atualiza o total. Ações explícitas (pagar/reabrir) usam [saveInvoice].
     */
    suspend fun saveGeneratedInvoice(invoice: Invoice) {
        val dao = invoiceDao ?: return
        val stored = dao.getById(invoice.id)
        if (stored != null && stored.status == "PAID" && invoice.status != "PAID") {
            dao.insert(
                stored.copy(
                    totalCents = invoice.totalCents,
                    closingDate = invoice.closingDate,
                    dueDate = invoice.dueDate,
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        } else {
            dao.insert(invoice)
        }
        onLocalChange()
    }

    suspend fun saveCardPurchase(
        description: String,
        amountCents: Long,
        accountId: String,
        installments: Int = 1,
        date: LocalDate = LocalDate.now(),
        categoryId: String? = null,
        notes: String? = null,
        status: String = "PAID",
    ) {
        val safeInstallments = installments.coerceIn(1, 48)
        val account = accountDao.getAllActive().first().firstOrNull { it.id == accountId }

        if (account == null || account.type != "CREDIT_CARD" || safeInstallments <= 1) {
            saveTransaction(
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    description = description,
                    amountCents = amountCents,
                    date = date,
                    status = status,
                    accountId = accountId,
                    categoryId = categoryId,
                    notes = notes,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
            return
        }

        val groupId = "installment-${UUID.randomUUID()}"
        val split = InstallmentSplitter.split(amountCents, safeInstallments, groupId)
        val now = System.currentTimeMillis()

        split.forEachIndexed { index, installment ->
            val transaction =
                Transaction(
                    id = UUID.randomUUID().toString(),
                    type = "EXPENSE",
                    description = description,
                    amountCents = installment.amountCents,
                    date = date.plusMonths(index.toLong()),
                    status = status,
                    accountId = accountId,
                    categoryId = categoryId,
                    installmentGroupId = groupId,
                    installmentNumber = installment.number,
                    installmentTotal = installment.totalInstallments,
                    notes = notes,
                    createdAt = now,
                    updatedAt = now,
                )
            saveTransaction(transaction)
        }
    }

    fun generateInvoicesForAccount(
        account: Account,
        transactions: List<Transaction>,
    ): List<Invoice> =
        InvoiceGenerator.generateForAccount(account, transactions)
            .map { it.toInvoice() }

    fun syncInvoicesForAccount(
        account: Account,
        transactions: List<Transaction>,
    ): List<Invoice> = generateInvoicesForAccount(account, transactions)

    suspend fun saveInvoice(invoice: Invoice) {
        invoiceDao?.insert(invoice)
        onLocalChange()
    }

    // --- Budgets ---

    fun getBudgets(): Flow<List<Budget>> = budgetDao?.getAllActive() ?: flowOf(emptyList())

    suspend fun saveBudget(budget: Budget) {
        budgetDao?.insert(budget)
        onLocalChange()
    }

    suspend fun deleteBudget(
        month: String,
        categoryId: String,
    ) {
        budgetDao?.deleteByMonthCategory(month, categoryId)
        onLocalChange()
    }

    // --- Goals ---

    fun getGoals(): Flow<List<Goal>> = goalDao?.getAllActive() ?: flowOf(emptyList())

    suspend fun saveGoal(goal: Goal) {
        goalDao?.insert(goal)
        onLocalChange()
    }

    suspend fun updateGoal(goal: Goal) {
        goalDao?.update(goal)
        onLocalChange()
    }

    suspend fun deleteGoal(id: String) {
        goalDao?.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }

    fun getGoalContributions(): Flow<List<GoalContribution>> =
        goalContributionDao?.getAllActive() ?: flowOf(emptyList())

    suspend fun saveGoalContribution(contribution: GoalContribution) {
        goalContributionDao?.insert(contribution)
        onLocalChange()
    }

    suspend fun deleteGoalContribution(id: String) {
        goalContributionDao?.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }

    // --- Recurrences ---

    fun getRecurrences(): Flow<List<Recurrence>> = recurrenceDao?.getAllActive() ?: flowOf(emptyList())

    suspend fun saveRecurrence(recurrence: Recurrence) {
        recurrenceDao?.insert(recurrence)
        onLocalChange()
    }

    suspend fun updateRecurrence(recurrence: Recurrence) {
        recurrenceDao?.update(recurrence)
        onLocalChange()
    }

    suspend fun deleteRecurrence(id: String) {
        recurrenceDao?.softDelete(id, System.currentTimeMillis())
        onLocalChange()
    }
}
