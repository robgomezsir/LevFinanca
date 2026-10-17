package com.example.lev.domain

import com.example.lev.data.model.Category
import com.example.lev.data.model.Transaction
import java.time.YearMonth

data class BudgetSnapshot(
    val categoryId: String,
    val categoryName: String,
    val plannedCents: Long,
    val spentCents: Long,
    val remainingCents: Long,
    val rolloverCents: Long = 0L,
    val percentUsed: Int = 0,
    val isOverBudget: Boolean = false,
)

object BudgetEngine {
    fun calculateMonthlyBudget(
        month: YearMonth,
        categories: List<Category>,
        transactions: List<Transaction>,
        plannedByCategory: Map<String, Long> = emptyMap(),
        previousCarryoverByCategory: Map<String, Long> = emptyMap(),
    ): List<BudgetSnapshot> {
        val roots = categories.filter { it.parentId == null }
        return roots.map { root ->
            val descendantIds = collectDescendantIds(root.id, categories)
            val spending =
                transactions.filter { tx ->
                    tx.status == "PAID" &&
                        tx.type == "EXPENSE" &&
                        tx.date.year == month.year &&
                        tx.date.monthValue == month.monthValue &&
                        (tx.categoryId in descendantIds || tx.categoryId == root.id)
                }.sumOf { it.amountCents }

            val planned =
                plannedByCategory.entries
                    .filter { (categoryId, _) -> categoryId == root.id || categoryId in descendantIds }
                    .sumOf { it.value }

            val rollover = previousCarryoverByCategory[root.id] ?: 0L
            val remaining =
                when {
                    planned == 0L && rollover == 0L -> 0L
                    else -> planned + rollover - spending
                }
            val percentUsed =
                if (planned == 0L) {
                    0
                } else {
                    ((spending.toDouble() / (planned + rollover).coerceAtLeast(1L)) * 100).toInt()
                }

            BudgetSnapshot(
                categoryId = root.id,
                categoryName = root.name,
                plannedCents = planned,
                spentCents = spending,
                remainingCents = remaining,
                rolloverCents = rollover,
                percentUsed = percentUsed,
                isOverBudget = remaining < 0L,
            )
        }
    }

    private fun collectDescendantIds(
        rootId: String,
        categories: List<Category>,
    ): Set<String> {
        val result = linkedSetOf<String>()
        val queue = ArrayDeque<String>()
        queue.add(rootId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            categories.filter { it.parentId == current }.forEach { child ->
                if (result.add(child.id)) {
                    queue.add(child.id)
                }
            }
        }
        return result
    }
}
