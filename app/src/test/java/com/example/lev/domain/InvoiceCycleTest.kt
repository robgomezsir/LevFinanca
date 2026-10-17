package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class InvoiceCycleTest {
    @Test
    fun `purchase exactly on closing day (inclusive)`() {
        val purchase = LocalDate.of(2026, 10, 25)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 25,
                dueDay = 5,
                closingInclusive = true,
            )
        assertEquals(LocalDate.of(2026, 10, 1), ref.referenceMonth)
        assertEquals(LocalDate.of(2026, 10, 25), ref.closingDate)
        assertEquals(LocalDate.of(2026, 11, 5), ref.dueDate)
    }

    @Test
    fun `purchase exactly on closing day (exclusive)`() {
        val purchase = LocalDate.of(2026, 10, 25)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 25,
                dueDay = 5,
                closingInclusive = false,
            )
        assertEquals(LocalDate.of(2026, 11, 1), ref.referenceMonth)
        assertEquals(LocalDate.of(2026, 11, 25), ref.closingDate)
        assertEquals(LocalDate.of(2026, 12, 5), ref.dueDate)
    }

    @Test
    fun `purchase the day after closing falls into next invoice`() {
        val purchase = LocalDate.of(2026, 10, 26)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 25,
                dueDay = 5,
                closingInclusive = true,
            )
        assertEquals(LocalDate.of(2026, 11, 1), ref.referenceMonth)
        assertEquals(LocalDate.of(2026, 11, 25), ref.closingDate)
        assertEquals(LocalDate.of(2026, 12, 5), ref.dueDate)
    }

    @Test
    fun `due day before closing pushes due date to next month`() {
        val purchase = LocalDate.of(2026, 10, 10)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 25,
                dueDay = 10,
            )
        assertEquals(LocalDate.of(2026, 10, 25), ref.closingDate)
        assertEquals(LocalDate.of(2026, 11, 10), ref.dueDate)
    }

    @Test
    fun `purchase on 31st Dec falls to next year`() {
        val purchase = LocalDate.of(2026, 12, 31)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 25,
                dueDay = 5,
            )
        assertEquals(LocalDate.of(2027, 1, 1), ref.referenceMonth)
        assertEquals(LocalDate.of(2027, 1, 25), ref.closingDate)
        assertEquals(LocalDate.of(2027, 2, 5), ref.dueDate)
    }

    @Test
    fun `leap year feb closing day safe clamping`() {
        val purchase = LocalDate.of(2024, 2, 10)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 30,
                dueDay = 10,
            )
        assertEquals(LocalDate.of(2024, 2, 29), ref.closingDate)
        assertEquals(LocalDate.of(2024, 3, 10), ref.dueDate)
    }

    @Test
    fun `closing day 28 in a short month clamps correctly`() {
        val purchase = LocalDate.of(2025, 2, 15)
        val ref =
            InvoiceCycle.calculateInvoiceForPurchase(
                purchaseDate = purchase,
                closingDay = 28,
                dueDay = 15,
            )
        assertEquals(LocalDate.of(2025, 2, 28), ref.closingDate)
        assertEquals(LocalDate.of(2025, 3, 15), ref.dueDate)
    }
}
