package com.example.lev.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentSplitterTest {
    @Test
    fun `split 100 in 3x gives 34, 33, 33`() {
        val total = 10000L // 100 reais
        val result = InstallmentSplitter.split(total, 3)

        assertEquals(3, result.size)
        assertEquals(3334L, result[0].amountCents)
        assertEquals(3333L, result[1].amountCents)
        assertEquals(3333L, result[2].amountCents)

        assertEquals(total, result.sumOf { it.amountCents })
    }

    @Test
    fun `split 0,10 in 3x gives 4, 3, 3`() {
        val total = 10L // 10 cents
        val result = InstallmentSplitter.split(total, 3)

        assertEquals(3, result.size)
        assertEquals(4L, result[0].amountCents)
        assertEquals(3L, result[1].amountCents)
        assertEquals(3L, result[2].amountCents)

        assertEquals(total, result.sumOf { it.amountCents })
    }

    @Test
    fun `split 1 installment keeps full total`() {
        val total = 1500L
        val result = InstallmentSplitter.split(total, 1)

        assertEquals(1, result.size)
        assertEquals(1500L, result[0].amountCents)
        assertEquals(1500L, result.sumOf { it.amountCents })
    }

    @Test
    fun `split large amount in 12x`() {
        val total = 100000L // 1000 reais
        val result = InstallmentSplitter.split(total, 12)

        assertEquals(12, result.size)
        assertEquals(total, result.sumOf { it.amountCents })
    }

    @Test
    fun `recalculate all installments after total change`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2500L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2500L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2500L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2500L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.recalculate(
                existingInstallments = original,
                newTotalAmountCents = 12000L,
                editedInstallmentNumber = 1,
                mode = InstallmentSplitter.EditMode.ALL,
                newGroupId = "grp-2",
            )

        assertEquals(4, updated.size)
        assertEquals(3000L, updated[0].amountCents)
        assertEquals(3000L, updated[1].amountCents)
        assertEquals(3000L, updated[2].amountCents)
        assertEquals(3000L, updated[3].amountCents)
        assertEquals(12000L, updated.sumOf { it.amountCents })
    }

    @Test
    fun `recalculate all installments must recalculate every parcel even when editing a later installment`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 1000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 1000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 1000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 1000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.recalculate(
                existingInstallments = original,
                newTotalAmountCents = 12000L,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.ALL,
                newGroupId = "grp-2",
            )

        assertEquals(listOf(1, 2, 3, 4), updated.map { it.number })
        assertEquals(listOf(3000L, 3000L, 3000L, 3000L), updated.map { it.amountCents })
        assertEquals(12000L, updated.sumOf { it.amountCents })
    }

    @Test
    fun `recalculate this only changes the selected installment and keeps the rest untouched`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.recalculate(
                existingInstallments = original,
                newTotalAmountCents = 5000L,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.THIS_ONLY,
                newGroupId = "grp-2",
            )

        assertEquals(2000L, updated[0].amountCents)
        assertEquals(5000L, updated[1].amountCents)
        assertEquals(2000L, updated[2].amountCents)
        assertEquals(2000L, updated[3].amountCents)
        assertEquals(11000L, updated.sumOf { it.amountCents })
    }

    @Test
    fun `recalculate this and following installments keeps earlier ones untouched`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.recalculate(
                existingInstallments = original,
                newTotalAmountCents = 6000L,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.THIS_AND_FOLLOWING,
                newGroupId = "grp-2",
            )

        assertEquals(2000L, updated[0].amountCents)
        assertEquals(2000L, updated[1].amountCents)
        assertEquals(2000L, updated[2].amountCents)
        assertEquals(2000L, updated[3].amountCents)
        assertEquals(8000L, updated.sumOf { it.amountCents })
    }

    @Test
    fun `delete this installment removes only the selected parcel and renumbers the remaining ones`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.delete(
                existingInstallments = original,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.THIS_ONLY,
                newGroupId = "grp-2",
            )

        assertEquals(3, updated.size)
        assertEquals(listOf(1, 2, 3), updated.map { it.number })
        assertEquals(listOf(2000L, 2000L, 2000L), updated.map { it.amountCents })
    }

    @Test
    fun `delete this and following keeps only earlier installments`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.delete(
                existingInstallments = original,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.THIS_AND_FOLLOWING,
                newGroupId = "grp-2",
            )

        assertEquals(1, updated.size)
        assertEquals(listOf(1), updated.map { it.number })
        assertEquals(listOf(2000L), updated.map { it.amountCents })
    }

    @Test
    fun `delete all removes every installment from the plan`() {
        val original =
            listOf(
                InstallmentSplitter.Installment(1, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(2, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(3, 2000L, 4, "grp-1"),
                InstallmentSplitter.Installment(4, 2000L, 4, "grp-1"),
            )

        val updated =
            InstallmentSplitter.delete(
                existingInstallments = original,
                editedInstallmentNumber = 2,
                mode = InstallmentSplitter.EditMode.ALL,
                newGroupId = "grp-2",
            )

        assertTrue(updated.isEmpty())
    }
}
