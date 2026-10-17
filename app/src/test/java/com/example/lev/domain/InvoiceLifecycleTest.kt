package com.example.lev.domain

import com.example.lev.data.model.Invoice
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InvoiceLifecycleTest {
    private fun invoice(
        total: Long,
        status: String = "OPEN",
        paidAmount: Long = 0L,
    ): Invoice =
        Invoice(
            id = "inv-1",
            accountId = "card-1",
            referenceMonth = LocalDate.of(2026, 10, 1),
            closingDate = LocalDate.of(2026, 10, 25),
            dueDate = LocalDate.of(2026, 11, 5),
            status = status,
            totalCents = total,
            paidAmountCents = paidAmount,
            createdAt = 0L,
            updatedAt = 0L,
        )

    @Test
    fun `total payment closes invoice as paid`() {
        val result = InvoiceLifecycle.pay(invoice(total = 1000L), amountCents = 1000L)

        assertEquals(1000L, result.paidAmountCents)
        assertEquals(0L, result.remainingBalanceCents)
        assertEquals(InvoiceLifecycle.Status.PAID, result.status)
        assertEquals("PAID", result.invoice.status)
    }

    @Test
    fun `partial payment keeps remaining balance`() {
        val result = InvoiceLifecycle.pay(invoice(total = 1000L), amountCents = 300L)

        assertEquals(300L, result.paidAmountCents)
        assertEquals(700L, result.remainingBalanceCents)
        assertEquals(InvoiceLifecycle.Status.OPEN, result.status)
    }

    @Test
    fun `reopen invoice clears paid amount`() {
        val reopened = InvoiceLifecycle.reopen(invoice(total = 1000L, status = "PAID", paidAmount = 1000L))
        assertEquals("OPEN", reopened.status)
        assertEquals(0L, reopened.paidAmountCents)
    }

    @Test
    fun `paid invoice is not payable and is reopenable`() {
        val paid = invoice(total = 1000L, status = "PAID", paidAmount = 1000L)

        assertEquals(false, InvoiceLifecycle.isPayable(paid))
        assertEquals(true, InvoiceLifecycle.canReopen(paid))
    }

    @Test
    fun `closed invoice with remaining balance is still payable but reopenable`() {
        val closed = invoice(total = 1000L, status = "CLOSED", paidAmount = 300L)

        assertEquals(true, InvoiceLifecycle.isPayable(closed))
        assertEquals(true, InvoiceLifecycle.canReopen(closed))
    }
}
