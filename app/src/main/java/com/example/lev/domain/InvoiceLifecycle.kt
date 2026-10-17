package com.example.lev.domain

import com.example.lev.data.model.Invoice

object InvoiceLifecycle {
    enum class Status {
        OPEN,
        CLOSED,
        PAID,
    }

    data class PaymentResult(
        val invoice: Invoice,
        val paidAmountCents: Long,
        val remainingBalanceCents: Long,
        val status: Status,
    )

    fun remainingBalance(invoice: Invoice): Long = invoice.totalCents - invoice.paidAmountCents

    fun isPayable(invoice: Invoice): Boolean {
        if (invoice.status == Status.PAID.name) return false

        val remainingBalance = remainingBalance(invoice)
        return remainingBalance > 0L
    }

    fun canReopen(invoice: Invoice): Boolean =
        invoice.status == Status.PAID.name || invoice.status == Status.CLOSED.name

    fun pay(
        invoice: Invoice,
        amountCents: Long,
    ): PaymentResult {
        require(amountCents >= 0L) { "Payment amount must be non-negative." }
        require(invoice.totalCents >= 0L) { "Invoice total must be non-negative." }

        val currentPaid = invoice.paidAmountCents
        val proposedPaid = currentPaid + amountCents
        val newPaid = minOf(proposedPaid, invoice.totalCents)
        val remaining = invoice.totalCents - newPaid

        val nextStatus =
            when {
                remaining <= 0L -> Status.PAID
                invoice.status == "CLOSED" -> Status.CLOSED
                else -> Status.OPEN
            }

        val updatedInvoice =
            invoice.copy(
                paidAmountCents = newPaid,
                status = nextStatus.name,
                updatedAt = System.currentTimeMillis(),
            )

        return PaymentResult(
            invoice = updatedInvoice,
            paidAmountCents = newPaid,
            remainingBalanceCents = remaining,
            status = nextStatus,
        )
    }

    fun reopen(invoice: Invoice): Invoice {
        require(canReopen(invoice)) {
            "Only closed/paid invoices can be reopened."
        }

        return invoice.copy(
            status = Status.OPEN.name,
            paidAmountCents = 0L,
            updatedAt = System.currentTimeMillis(),
        )
    }
}
