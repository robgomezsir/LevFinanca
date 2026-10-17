package com.example.lev.data.repository

import com.example.lev.data.local.AccountDao
import com.example.lev.data.local.CategoryDao
import com.example.lev.data.local.InvoiceDao
import com.example.lev.data.local.TransactionDao
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FinanceRepositorySyncTest {
    private class FakeAccountDao : AccountDao {
        override fun getAllActive(): Flow<List<Account>> = flowOf(emptyList())

        override suspend fun insert(account: Account) = Unit

        override suspend fun update(account: Account) = Unit

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun getAllForSync(): List<Account> = emptyList()

        override suspend fun getDirty(): List<Account> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Account>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = 0

        override suspend fun clearAll() = Unit
    }

    private class FakeCategoryDao : CategoryDao {
        override fun getAllActive(): Flow<List<Category>> = flowOf(emptyList())

        override suspend fun insert(category: Category) = Unit

        override suspend fun update(category: Category) = Unit

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun getAllForSync(): List<Category> = emptyList()

        override suspend fun getDirty(): List<Category> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Category>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = 0

        override suspend fun clearAll() = Unit
    }

    private class FakeTransactionDao : TransactionDao {
        override fun getAllActive(): Flow<List<Transaction>> = flowOf(emptyList())

        override suspend fun insert(transaction: Transaction) = Unit

        override suspend fun update(transaction: Transaction) = Unit

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun getAllForSync(): List<Transaction> = emptyList()

        override suspend fun getDirty(): List<Transaction> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Transaction>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = 0

        override suspend fun clearAll() = Unit
    }

    private class RecordingAccountDao(
        private val accounts: MutableList<Account>,
    ) : AccountDao {
        override fun getAllActive(): Flow<List<Account>> = flowOf(accounts.toList())

        override suspend fun insert(account: Account) {
            accounts += account
        }

        override suspend fun update(account: Account) {
            val idx = accounts.indexOfFirst { it.id == account.id }
            if (idx >= 0) {
                accounts[idx] = account
            }
        }

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) {
            accounts.removeAll { it.id == id }
        }

        override suspend fun getAllForSync(): List<Account> = accounts.toList()

        override suspend fun getDirty(): List<Account> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Account>) {
            rows.forEach { update(it) }
        }

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = accounts.size

        override suspend fun clearAll() {
            accounts.clear()
        }
    }

    private class RecordingTransactionDao(
        val transactions: MutableList<Transaction>,
    ) : TransactionDao {
        override fun getAllActive(): Flow<List<Transaction>> = flowOf(transactions.toList())

        override suspend fun insert(transaction: Transaction) {
            transactions += transaction
        }

        override suspend fun update(transaction: Transaction) {
            val idx = transactions.indexOfFirst { it.id == transaction.id }
            if (idx >= 0) {
                transactions[idx] = transaction
            }
        }

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) {
            transactions.removeAll { it.id == id }
        }

        override suspend fun getAllForSync(): List<Transaction> = transactions.toList()

        override suspend fun getDirty(): List<Transaction> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Transaction>) {
            rows.forEach { update(it) }
        }

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = transactions.size

        override suspend fun clearAll() {
            transactions.clear()
        }
    }

    private class RecordingInvoiceDao : InvoiceDao {
        val inserted: MutableList<Invoice> = mutableListOf()

        override fun getInvoicesForAccount(accountId: String): Flow<List<Invoice>> =
            flowOf(
                inserted.filter {
                    it.accountId == accountId
                },
            )

        override fun getAll(): Flow<List<Invoice>> = flowOf(inserted.toList())

        override suspend fun getById(id: String): Invoice? = inserted.firstOrNull { it.id == id }

        override suspend fun insert(invoice: Invoice) {
            inserted.removeAll { it.id == invoice.id }
            inserted += invoice
        }

        override suspend fun getAllForSync(): List<Invoice> = inserted.toList()

        override suspend fun getDirty(): List<Invoice> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Invoice>) {
            inserted += rows
        }

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = inserted.size

        override suspend fun clearAll() {
            inserted.clear()
        }
    }

    private fun cardAccount(): Account =
        Account(
            id = "card-1",
            name = "Nubank",
            type = "CREDIT_CARD",
            initialBalanceCents = 0L,
            currency = "BRL",
            color = "#000000",
            icon = "card",
            creditLimitCents = 300000L,
            closingDay = 25,
            dueDay = 5,
            closingInclusive = true,
            paymentAccountId = "checking-1",
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun expense(
        date: LocalDate,
        amount: Long,
        installmentNumber: Int? = null,
        installmentTotal: Int? = null,
    ): Transaction =
        Transaction(
            id = "$date-${installmentNumber ?: 0}",
            type = "EXPENSE",
            description = "Compra",
            amountCents = amount,
            date = date,
            status = "PAID",
            accountId = "card-1",
            installmentGroupId = if (installmentNumber != null) "group-$date" else null,
            installmentNumber = installmentNumber,
            installmentTotal = installmentTotal,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `sync invoices groups expenses by invoice cycle`() {
        val repo =
            FinanceRepository(
                accountDao = FakeAccountDao(),
                categoryDao = FakeCategoryDao(),
                transactionDao = FakeTransactionDao(),
                invoiceDao =
                    object : InvoiceDao {
                        override fun getInvoicesForAccount(accountId: String): Flow<List<Invoice>> = flowOf(emptyList())

                        override fun getAll(): Flow<List<Invoice>> = flowOf(emptyList())

                        override suspend fun getById(id: String): Invoice? = null

                        override suspend fun insert(invoice: Invoice) = Unit

                        override suspend fun getAllForSync(): List<Invoice> = emptyList()

                        override suspend fun getDirty(): List<Invoice> = emptyList()

                        override suspend fun markSynced(
                            ids: List<String>,
                            syncedAt: Long,
                        ) = Unit

                        override suspend fun upsertAll(rows: List<Invoice>) = Unit

                        override suspend fun purgeTombstones(before: Long) = Unit

                        override suspend fun countAll(): Int = 0

                        override suspend fun clearAll() = Unit
                    },
            )

        val invoices =
            repo.syncInvoicesForAccount(
                account = cardAccount(),
                transactions =
                    listOf(
                        expense(LocalDate.of(2026, 10, 10), 1000L),
                        expense(LocalDate.of(2026, 10, 26), 3000L),
                        expense(LocalDate.of(2026, 11, 3), 1200L, installmentNumber = 2, installmentTotal = 3),
                    ),
            )

        assertEquals(3, invoices.size)
        assertEquals(1000L, invoices.first { it.referenceMonth == LocalDate.of(2026, 10, 1) }.totalCents)
        assertEquals(3000L, invoices.first { it.referenceMonth == LocalDate.of(2026, 11, 1) }.totalCents)
        assertEquals(1200L, invoices.first { it.referenceMonth == LocalDate.of(2026, 12, 1) }.totalCents)
    }

    @Test
    fun `saving a credit card expense auto syncs invoice entries`() =
        runBlocking {
            val account = cardAccount()
            val transactionDao = RecordingTransactionDao(mutableListOf())
            val invoiceDao = RecordingInvoiceDao()
            val repo =
                FinanceRepository(
                    accountDao = RecordingAccountDao(mutableListOf(account)),
                    categoryDao = FakeCategoryDao(),
                    transactionDao = transactionDao,
                    invoiceDao = invoiceDao,
                )

            repo.saveTransaction(
                expense(LocalDate.of(2026, 10, 10), 1000L),
            )

            assertEquals(1, invoiceDao.inserted.size)
            assertEquals(LocalDate.of(2026, 10, 1), invoiceDao.inserted.single().referenceMonth)
            assertEquals(1000L, invoiceDao.inserted.single().totalCents)
        }

    @Test
    fun `saving a credit card purchase with installments creates one transaction per parcel`() =
        runBlocking {
            val account = cardAccount()
            val transactionDao = RecordingTransactionDao(mutableListOf())
            val repo =
                FinanceRepository(
                    accountDao = RecordingAccountDao(mutableListOf(account)),
                    categoryDao = FakeCategoryDao(),
                    transactionDao = transactionDao,
                    invoiceDao = RecordingInvoiceDao(),
                )

            repo.saveCardPurchase(
                description = "Notebook",
                amountCents = 10000L,
                accountId = "card-1",
                installments = 3,
                date = LocalDate.of(2026, 10, 10),
            )

            assertEquals(3, transactionDao.transactions.size)
            assertEquals(listOf(1, 2, 3), transactionDao.transactions.map { it.installmentNumber })
            assertEquals(3, transactionDao.transactions.first().installmentTotal)
            assertEquals(10000L, transactionDao.transactions.sumOf { it.amountCents })
            assertEquals(1, transactionDao.transactions.map { it.installmentGroupId }.distinct().size)
        }

    @Test
    fun `installment purchases are assigned to the correct invoice cycles across months`() =
        runBlocking {
            val account = cardAccount()
            val transactionDao = RecordingTransactionDao(mutableListOf())
            val repo =
                FinanceRepository(
                    accountDao = RecordingAccountDao(mutableListOf(account)),
                    categoryDao = FakeCategoryDao(),
                    transactionDao = transactionDao,
                    invoiceDao = RecordingInvoiceDao(),
                )

            repo.saveCardPurchase(
                description = "Notebook",
                amountCents = 30000L,
                accountId = "card-1",
                installments = 3,
                date = LocalDate.of(2026, 10, 10),
            )

            assertEquals(
                listOf(
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 11, 10),
                    LocalDate.of(2026, 12, 10),
                ),
                transactionDao.transactions.sortedBy { it.installmentNumber }.map { it.date },
            )
        }
}
