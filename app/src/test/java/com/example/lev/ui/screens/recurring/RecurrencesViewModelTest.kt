package com.example.lev.ui.screens.recurring

import com.example.lev.data.model.Account
import com.example.lev.data.model.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Uma recorrência pausada precisa continuar visível: se ela desaparece da lista, o usuário não
 * consegue mais retomá-la nem excluí-la.
 */
class RecurrencesViewModelTest {
    private val today = LocalDate.of(2026, 10, 11)

    @Test
    fun `pausada vai para a lista de pausadas e nao para a de ativas`() {
        val lists =
            buildRecurrenceLists(
                recurrences =
                    listOf(
                        recurrence(id = "ativa", active = true),
                        recurrence(id = "pausada", active = false),
                    ),
                accounts = listOf(account()),
                categories = emptyList(),
                transactions = emptyList(),
                today = today,
            )

        assertEquals(listOf("ativa"), lists.active.map { it.recurrence.id })
        assertEquals(listOf("pausada"), lists.paused.map { it.recurrence.id })
    }

    @Test
    fun `pausada mantem os dados de exibicao que a ativa tem`() {
        val lists =
            buildRecurrenceLists(
                recurrences = listOf(recurrence(id = "pausada", active = false)),
                accounts = listOf(account()),
                categories = emptyList(),
                transactions = emptyList(),
                today = today,
            )

        val item = lists.paused.single()
        assertEquals("Carteira", item.accountName)
        assertEquals(150000L, item.recurrence.amountCents)
        assertTrue(item.nextOccurrences.isNotEmpty())
    }

    @Test
    fun `lista de ativas continua vazia quando so ha pausadas`() {
        val lists =
            buildRecurrenceLists(
                recurrences = listOf(recurrence(id = "pausada", active = false)),
                accounts = listOf(account()),
                categories = emptyList(),
                transactions = emptyList(),
                today = today,
            )

        assertTrue(lists.active.isEmpty())
        assertEquals(1, lists.paused.size)
    }

    private fun account() =
        Account(
            id = "acc-1",
            name = "Carteira",
            type = "CHECKING",
            initialBalanceCents = 0L,
            color = "#006C5B",
            icon = "account_balance_wallet",
            createdAt = 0L,
            updatedAt = 0L,
        )

    private fun recurrence(
        id: String,
        active: Boolean,
    ) = Recurrence(
        id = id,
        type = "EXPENSE",
        description = "Aluguel",
        amountCents = 150000L,
        accountId = "acc-1",
        frequency = "MONTHLY",
        startDate = today,
        active = active,
        createdAt = 0L,
        updatedAt = 0L,
    )
}
