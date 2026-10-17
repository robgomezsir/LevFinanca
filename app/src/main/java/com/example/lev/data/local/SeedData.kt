package com.example.lev.data.local

import com.example.lev.data.model.Category
import java.util.UUID

object SeedData {
    private fun createCategory(
        name: String,
        kind: String,
        color: String,
        icon: String,
    ): Category {
        val now = System.currentTimeMillis()
        return Category(
            id = UUID.randomUUID().toString(),
            name = name,
            kind = kind,
            color = color,
            icon = icon,
            createdAt = now,
            updatedAt = now,
        )
    }

    val defaultCategories =
        listOf(
            // Income
            createCategory("Salário", "INCOME", "#00C853", "attach_money"),
            createCategory("Freelance", "INCOME", "#00E676", "work"),
            createCategory("Rendimentos", "INCOME", "#69F0AE", "trending_up"),
            createCategory("Outras Receitas", "INCOME", "#B9F6CA", "add_circle"),
            // Expenses
            createCategory("Moradia", "EXPENSE", "#D50000", "home"),
            createCategory("Mercado", "EXPENSE", "#FF1744", "shopping_cart"),
            createCategory("Alimentação fora", "EXPENSE", "#FF5252", "restaurant"),
            createCategory("Transporte", "EXPENSE", "#FF8A80", "directions_car"),
            createCategory("Saúde", "EXPENSE", "#C51162", "favorite"),
            createCategory("Educação", "EXPENSE", "#F50057", "school"),
            createCategory("Lazer", "EXPENSE", "#FF4081", "sports_esports"),
            createCategory("Assinaturas", "EXPENSE", "#6200EA", "subscriptions"),
            createCategory("Compras", "EXPENSE", "#AA00FF", "shopping_bag"),
            createCategory("Impostos e taxas", "EXPENSE", "#D500F9", "account_balance"),
            createCategory("Outras Despesas", "EXPENSE", "#E040FB", "remove_circle"),
        )
}
