package com.example.lev.ui.screens.transactions

/** Result of a fast entry submission from the bottom sheet. */
data class FastEntryResult(
    val amountString: String,
    val type: String,
    val accountId: String,
    val categoryId: String?,
    val installments: Int,
    val description: String,
)
