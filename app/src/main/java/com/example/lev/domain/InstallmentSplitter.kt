package com.example.lev.domain

import kotlin.math.abs

object InstallmentSplitter {
    enum class EditMode {
        THIS_ONLY,
        THIS_AND_FOLLOWING,
        ALL,
    }

    data class Installment(
        val number: Int,
        val amountCents: Long,
        val totalInstallments: Int = 0,
        val groupId: String = "",
    )

    /**
     * Splits a total amount into a specific number of installments.
     * Handles remaining cents by distributing 1 cent to the earliest installments.
     * E.g., 1000 cents (R$ 10,00) in 3x -> 334, 333, 333
     */
    fun split(
        totalAmountCents: Long,
        installments: Int,
        groupId: String? = null,
    ): List<Installment> {
        require(installments > 0) { "Installments must be greater than zero." }

        val sign = if (totalAmountCents < 0) -1L else 1L
        val absoluteTotal = abs(totalAmountCents)
        val baseAmount = absoluteTotal / installments
        val remainder = (absoluteTotal % installments).toInt()
        val resolvedGroupId = groupId ?: "installment-$absoluteTotal-$installments"

        return (1..installments).map { number ->
            val extraCent = if (number <= remainder) 1L else 0L
            val amount = (baseAmount + extraCent) * sign
            Installment(
                number = number,
                amountCents = amount,
                totalInstallments = installments,
                groupId = resolvedGroupId,
            )
        }
    }

    /**
     * Recalculates the amount for a selected installment or a subset of a plan after the total value changes.
     *
     * THIS_ONLY: recalculates only the selected installment.
     * THIS_AND_FOLLOWING: recalculates the selected installment and all subsequent ones.
     * ALL: recalculates the full series.
     */
    fun recalculate(
        existingInstallments: List<Installment>,
        newTotalAmountCents: Long,
        editedInstallmentNumber: Int,
        mode: EditMode,
        newGroupId: String? = null,
    ): List<Installment> {
        require(existingInstallments.isNotEmpty()) { "Installments cannot be empty." }
        require(editedInstallmentNumber in 1..existingInstallments.size) {
            "editedInstallmentNumber must be between 1 and ${existingInstallments.size}."
        }

        val ordered = existingInstallments.sortedBy { it.number }
        val groupId = newGroupId ?: ordered.first().groupId

        if (mode == EditMode.ALL) {
            return split(newTotalAmountCents, ordered.size, groupId = groupId)
                .mapIndexed { index, item ->
                    item.copy(
                        number = index + 1,
                        totalInstallments = ordered.size,
                    )
                }
        }

        val affectedCount =
            when (mode) {
                EditMode.THIS_ONLY -> 1
                EditMode.THIS_AND_FOLLOWING -> ordered.size - editedInstallmentNumber + 1
                EditMode.ALL -> ordered.size
            }

        val unaffectedPrefix = ordered.take(editedInstallmentNumber - 1)
        val changedSeries =
            split(newTotalAmountCents, affectedCount, groupId = groupId)
                .mapIndexed { index, item ->
                    item.copy(
                        number = editedInstallmentNumber + index,
                        totalInstallments = ordered.size,
                    )
                }

        val keptFollowing =
            when (mode) {
                EditMode.ALL -> emptyList()
                else -> ordered.drop(editedInstallmentNumber + affectedCount - 1)
            }

        val finalList = mutableListOf<Installment>()
        finalList.addAll(unaffectedPrefix)
        finalList.addAll(changedSeries)
        finalList.addAll(keptFollowing)
        return finalList.sortedBy { it.number }
    }

    fun delete(
        existingInstallments: List<Installment>,
        editedInstallmentNumber: Int,
        mode: EditMode,
        newGroupId: String? = null,
    ): List<Installment> {
        require(existingInstallments.isNotEmpty()) { "Installments cannot be empty." }
        require(editedInstallmentNumber in 1..existingInstallments.size) {
            "editedInstallmentNumber must be between 1 and ${existingInstallments.size}."
        }

        val ordered = existingInstallments.sortedBy { it.number }
        val groupId = newGroupId ?: ordered.first().groupId

        val retained =
            ordered.filterIndexed { index, _ ->
                val valueIndex = index + 1
                when (mode) {
                    EditMode.THIS_ONLY -> valueIndex != editedInstallmentNumber
                    EditMode.THIS_AND_FOLLOWING -> valueIndex < editedInstallmentNumber
                    EditMode.ALL -> false
                }
            }

        return retained.mapIndexed { index, item ->
            item.copy(
                number = index + 1,
                totalInstallments = retained.size,
                groupId = groupId,
            )
        }
    }

    fun remove(
        existingInstallments: List<Installment>,
        editedInstallmentNumber: Int,
        mode: EditMode,
        newGroupId: String? = null,
    ): List<Installment> = delete(existingInstallments, editedInstallmentNumber, mode, newGroupId)
}
