package com.example.lev.domain

import java.time.LocalDate

object InvoiceCycle {
    data class InvoiceRef(
        val referenceMonth: LocalDate,
        val closingDate: LocalDate,
        val dueDate: LocalDate,
    )

    /**
     * Determines which invoice a purchase falls into.
     * @param purchaseDate Date of the purchase.
     * @param closingDay The day the invoice closes (e.g., 25).
     * @param dueDay The day the invoice is due (e.g., 5).
     * @param closingInclusive If true, purchases on closingDay go to the CURRENT invoice. If false, NEXT invoice.
     * @param monthsAhead For installments. 0 = current invoice, 1 = next invoice, etc.
     */
    fun calculateInvoiceForPurchase(
        purchaseDate: LocalDate,
        closingDay: Int,
        dueDay: Int,
        closingInclusive: Boolean = true,
        monthsAhead: Int = 0,
    ): InvoiceRef {
        require(closingDay in 1..31) { "closingDay must be between 1 and 31" }
        require(dueDay in 1..31) { "dueDay must be between 1 and 31" }

        val monthStart = purchaseDate.withDayOfMonth(1)
        val thisMonthClosingDate = getSafeDate(purchaseDate.year, purchaseDate.monthValue, closingDay)

        val belongsToCurrentInvoice =
            if (closingInclusive) {
                purchaseDate <= thisMonthClosingDate
            } else {
                purchaseDate < thisMonthClosingDate
            }

        var baseInvoiceMonth = monthStart
        if (!belongsToCurrentInvoice) {
            baseInvoiceMonth = baseInvoiceMonth.plusMonths(1)
        }

        baseInvoiceMonth = baseInvoiceMonth.plusMonths(monthsAhead.toLong())

        val finalClosingDate = getSafeDate(baseInvoiceMonth.year, baseInvoiceMonth.monthValue, closingDay)

        var dueDateMonth = baseInvoiceMonth
        if (dueDay < closingDay) {
            dueDateMonth = dueDateMonth.plusMonths(1)
        }
        val finalDueDate = getSafeDate(dueDateMonth.year, dueDateMonth.monthValue, dueDay)

        return InvoiceRef(
            referenceMonth = baseInvoiceMonth,
            closingDate = finalClosingDate,
            dueDate = finalDueDate,
        )
    }

    private fun getSafeDate(
        year: Int,
        month: Int,
        day: Int,
    ): LocalDate {
        val tempDate = LocalDate.of(year, month, 1)
        val maxDays = tempDate.lengthOfMonth()
        val safeDay = minOf(day, maxDays)
        return LocalDate.of(year, month, safeDay)
    }
}
