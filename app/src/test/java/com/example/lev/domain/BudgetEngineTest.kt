package com.example.lev.domain

import com.example.lev.data.model.Category
import com.example.lev.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class BudgetEngineTest {
    private fun category(
        id: String,
        name: String,
        parentId: String? = null,
        kind: String = "EXPENSE",
    ) = Category(
        id = id,
        name = name,
        kind = kind,
        parentId = parentId,
        color = "#000000",
        icon = "wallet",
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun tx(
        id: String,
        categoryId: String,
        amountCents: Long,
        date: LocalDate,
        accountId: String = "a1",
        type: String = "EXPENSE",
        status: String = "PAID",
    ) = Transaction(
        id = id,
        type = type,
        description = "tx-$id",
        amountCents = amountCents,
        date = date,
        status = status,
        accountId = accountId,
        categoryId = categoryId,
        createdAt = 0L,
        updatedAt = 0L,
    )

    @Test
    fun `budget engine rolls over positive and negative balances by category`() {
        val categories =
            listOf(
                category("food", "Food"),
                category("groceries", "Groceries", parentId = "food"),
                category("restaurants", "Restaurants", parentId = "food"),
            )

        val transactions =
            listOf(
                tx("t1", "groceries", 400000L, LocalDate.of(2026, 7, 10)),
                tx("t2", "restaurants", 320000L, LocalDate.of(2026, 7, 18)),
            )

        val snapshots =
            BudgetEngine.calculateMonthlyBudget(
                month = YearMonth.of(2026, 7),
                categories = categories,
                transactions = transactions,
                plannedByCategory = mapOf("food" to 1000000L),
                previousCarryoverByCategory = mapOf("food" to 150000L),
            )

        val food = snapshots.single { it.categoryId == "food" }
        assertEquals(1000000L, food.plannedCents)
        assertEquals(720000L, food.spentCents)
        assertEquals(430000L, food.remainingCents)
        assertEquals(150000L, food.rolloverCents)
    }

    @Test
    fun `budget engine sums subcategory values into the parent category`() {
        val categories =
            listOf(
                category("food", "Food"),
                category("groceries", "Groceries", parentId = "food"),
                category("restaurants", "Restaurants", parentId = "food"),
            )

        val transactions =
            listOf(
                tx("a", "groceries", 250000L, LocalDate.of(2026, 7, 4)),
                tx("b", "restaurants", 120000L, LocalDate.of(2026, 7, 12)),
            )

        val snapshots =
            BudgetEngine.calculateMonthlyBudget(
                month = YearMonth.of(2026, 7),
                categories = categories,
                transactions = transactions,
                plannedByCategory =
                    mapOf(
                        "groceries" to 500000L,
                        "restaurants" to 300000L,
                    ),
            )

        val food = snapshots.single { it.categoryId == "food" }
        assertEquals(800000L, food.plannedCents)
        assertEquals(370000L, food.spentCents)
        assertEquals(430000L, food.remainingCents)
    }

    @Test
    fun `budget engine returns zero planned budget for categories without allocation`() {
        val categories =
            listOf(
                category("transport", "Transport"),
                category("car", "Car", parentId = "transport"),
            )

        val snapshots =
            BudgetEngine.calculateMonthlyBudget(
                month = YearMonth.of(2026, 8),
                categories = categories,
                transactions =
                    listOf(
                        tx("fuel", "car", 90000L, LocalDate.of(2026, 8, 5)),
                    ),
                plannedByCategory = emptyMap(),
            )

        val transport = snapshots.single { it.categoryId == "transport" }
        assertEquals(0L, transport.plannedCents)
        assertEquals(90000L, transport.spentCents)
        assertEquals(0L, transport.remainingCents)
    }
}
