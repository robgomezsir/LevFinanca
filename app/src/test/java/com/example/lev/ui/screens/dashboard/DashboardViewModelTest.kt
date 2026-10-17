package com.example.lev.ui.screens.dashboard

import com.example.lev.data.local.AccountDao
import com.example.lev.data.local.CategoryDao
import com.example.lev.data.local.InvoiceDao
import com.example.lev.data.local.TransactionDao
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.Transaction
import com.example.lev.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeAccounts(val rows: MutableList<Account>) : AccountDao {
        private val flow = MutableStateFlow<List<Account>>(rows.toList())

        override fun getAllActive(): Flow<List<Account>> = flow

        override suspend fun insert(account: Account) {
            rows += account
            flow.value = rows.toList()
        }

        override suspend fun update(account: Account) = Unit

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun getAllForSync(): List<Account> = rows.toList()

        override suspend fun getDirty(): List<Account> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Account>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = rows.size

        override suspend fun clearAll() = Unit
    }

    private class FakeTransactions(val rows: MutableList<Transaction>) : TransactionDao {
        private val flow = MutableStateFlow<List<Transaction>>(rows.toList())

        override fun getAllActive(): Flow<List<Transaction>> = flow

        override suspend fun insert(transaction: Transaction) {
            rows += transaction
            flow.value = rows.toList()
        }

        override suspend fun update(transaction: Transaction) = Unit

        override suspend fun softDelete(
            id: String,
            updatedAt: Long,
        ) = Unit

        override suspend fun getAllForSync(): List<Transaction> = rows.toList()

        override suspend fun getDirty(): List<Transaction> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Transaction>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = rows.size

        override suspend fun clearAll() = Unit
    }

    private class FakeCategories : CategoryDao {
        override fun getAllActive(): Flow<List<Category>> = kotlinx.coroutines.flow.flowOf(emptyList())

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

    private class FakeInvoices(val rows: MutableList<Invoice>) : InvoiceDao {
        private val flow = MutableStateFlow<List<Invoice>>(rows.toList())

        override fun getInvoicesForAccount(accountId: String): Flow<List<Invoice>> =
            kotlinx.coroutines.flow.flowOf(rows.filter { it.accountId == accountId })

        override fun getAll(): Flow<List<Invoice>> = flow

        override suspend fun getById(id: String): Invoice? = rows.firstOrNull { it.id == id }

        override suspend fun insert(invoice: Invoice) {
            rows.removeAll { it.id == invoice.id }
            rows += invoice
            flow.value = rows.toList()
        }

        override suspend fun getAllForSync(): List<Invoice> = rows.toList()

        override suspend fun getDirty(): List<Invoice> = emptyList()

        override suspend fun markSynced(
            ids: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<Invoice>) = Unit

        override suspend fun purgeTombstones(before: Long) = Unit

        override suspend fun countAll(): Int = rows.size

        override suspend fun clearAll() = Unit
    }

    private fun checking(
        id: String,
        initial: Long,
    ): Account =
        Account(
            id = id,
            name = id,
            type = "CHECKING",
            initialBalanceCents = initial,
            currency = "BRL",
            color = "#000",
            icon = "i",
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun tx(
        id: String,
        type: String,
        amount: Long,
        accountId: String,
        status: String = "PAID",
        dest: String? = null,
        date: LocalDate = LocalDate.of(2026, 5, 10),
    ): Transaction =
        Transaction(
            id = id,
            type = type,
            description = id,
            amountCents = amount,
            date = date,
            status = status,
            accountId = accountId,
            destinationAccountId = dest,
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun repo(
        accounts: MutableList<Account>,
        transactions: MutableList<Transaction>,
        invoices: MutableList<Invoice> = mutableListOf(),
    ): FinanceRepository =
        FinanceRepository(
            accountDao = FakeAccounts(accounts),
            categoryDao = FakeCategories(),
            transactionDao = FakeTransactions(transactions),
            invoiceDao = FakeInvoices(invoices),
        )

    @Test
    fun `saldo da conta reflete lançamentos pagos e ignora pendentes`() =
        runTest(dispatcher) {
            val viewModel =
                DashboardViewModel(
                    repo(
                        mutableListOf(checking("c1", 10000L)),
                        mutableListOf(
                            tx("t1", "INCOME", 5000L, "c1"),
                            tx("t2", "EXPENSE", 2000L, "c1"),
                            tx("t3", "EXPENSE", 9000L, "c1", status = "PENDING"),
                        ),
                    ),
                )
            advanceUntilIdle()

            val card = viewModel.uiState.first { it.cards.isNotEmpty() }.cards.single()
            assertEquals(13000L, card.currentBalanceCents)
        }

    @Test
    fun `transferência move saldo entre contas`() =
        runTest(dispatcher) {
            val viewModel =
                DashboardViewModel(
                    repo(
                        mutableListOf(checking("c1", 10000L), checking("c2", 0L)),
                        mutableListOf(tx("t1", "TRANSFER", 3000L, "c1", dest = "c2")),
                    ),
                )
            advanceUntilIdle()

            val cards = viewModel.uiState.first { it.cards.size == 2 }.cards
            assertEquals(7000L, cards.first { it.account.id == "c1" }.currentBalanceCents)
            assertEquals(3000L, cards.first { it.account.id == "c2" }.currentBalanceCents)
        }

    @Test
    fun `fatura do cartão mostra o ciclo corrente e some ao pagar`() =
        runTest(dispatcher) {
            val today = LocalDate.now()
            val card =
                Account(
                    id = "card",
                    name = "Cartão",
                    type = "CREDIT_CARD",
                    initialBalanceCents = 0L,
                    currency = "BRL",
                    color = "#000",
                    icon = "c",
                    creditLimitCents = 100000L,
                    closingDay = 28,
                    dueDay = 10,
                    createdAt = 0L,
                    updatedAt = 0L,
                )
            val invoiceDao = FakeInvoices(mutableListOf())
            val repository =
                FinanceRepository(
                    accountDao = FakeAccounts(mutableListOf(card)),
                    categoryDao = FakeCategories(),
                    transactionDao =
                        FakeTransactions(
                            mutableListOf(tx("t1", "EXPENSE", 15000L, "card", date = today)),
                        ),
                    invoiceDao = invoiceDao,
                )
            val viewModel = DashboardViewModel(repository)
            advanceUntilIdle()

            var cardItem = viewModel.uiState.first { it.cards.isNotEmpty() }.cards.single()
            assertEquals(15000L, cardItem.currentInvoiceTotalCents)
            assertEquals(15000L, cardItem.usedCents)
            assertEquals(85000L, cardItem.availableCents)

            // Paga a fatura do ciclo corrente: some da aberta e do usado.
            val ref =
                com.example.lev.domain.InvoiceCycle.calculateInvoiceForPurchase(
                    purchaseDate = today,
                    closingDay = 28,
                    dueDay = 10,
                    closingInclusive = true,
                ).referenceMonth
            invoiceDao.insert(
                Invoice(
                    id = "invoice-card-${ref.year}-${ref.monthValue}",
                    accountId = "card",
                    referenceMonth = ref,
                    closingDate = ref,
                    dueDate = ref,
                    status = "PAID",
                    totalCents = 15000L,
                    createdAt = 0L,
                    updatedAt = 0L,
                ),
            )
            advanceUntilIdle()

            cardItem = viewModel.uiState.first { it.cards.single().currentInvoiceTotalCents == 0L }.cards.single()
            assertEquals(0L, cardItem.usedCents)
            assertEquals(100000L, cardItem.availableCents)
        }

    @Test
    fun `fatura gerada não ressuscita fatura paga`() =
        runTest(dispatcher) {
            val invoices = FakeInvoices(mutableListOf())
            val repository =
                FinanceRepository(
                    accountDao = FakeAccounts(mutableListOf()),
                    categoryDao = FakeCategories(),
                    transactionDao = FakeTransactions(mutableListOf()),
                    invoiceDao = invoices,
                )
            val paid =
                Invoice(
                    id = "inv-1",
                    accountId = "card",
                    referenceMonth = LocalDate.of(2026, 5, 1),
                    closingDate = LocalDate.of(2026, 5, 28),
                    dueDate = LocalDate.of(2026, 6, 10),
                    status = "PAID",
                    totalCents = 15000L,
                    paidAmountCents = 15000L,
                    createdAt = 0L,
                    updatedAt = 0L,
                )
            invoices.insert(paid)

            repository.saveGeneratedInvoice(paid.copy(status = "OPEN", totalCents = 20000L, paidAmountCents = 0L))

            val stored = invoices.getById("inv-1")!!
            assertEquals("PAID", stored.status)
            assertEquals(15000L, stored.paidAmountCents)
            assertEquals(20000L, stored.totalCents)
        }
}
