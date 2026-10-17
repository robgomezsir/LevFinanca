package com.example.lev.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "account")
data class Account(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // CHECKING, SAVINGS, CASH, CREDIT_CARD, INVESTMENT, BENEFIT, LOAN
    val initialBalanceCents: Long,
    val currency: String = "BRL",
    val color: String,
    val icon: String,
    val includeInTotals: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val creditLimitCents: Long? = null,
    val closingDay: Int? = null,
    val dueDay: Int? = null,
    val closingInclusive: Boolean = true,
    val paymentAccountId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "category")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String, // INCOME, EXPENSE
    val parentId: String? = null,
    val color: String,
    val icon: String,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "transaction")
data class Transaction(
    @PrimaryKey val id: String,
    val type: String, // INCOME, EXPENSE, TRANSFER, ADJUSTMENT
    val description: String,
    val amountCents: Long,
    val date: LocalDate,
    val dueDate: LocalDate? = null,
    val paidDate: LocalDate? = null,
    val status: String, // PENDING, PAID
    val accountId: String,
    val destinationAccountId: String? = null,
    val categoryId: String? = null,
    val payee: String? = null,
    val notes: String? = null,
    val installmentGroupId: String? = null,
    val installmentNumber: Int? = null,
    val installmentTotal: Int? = null,
    val recurrenceId: String? = null,
    val invoiceId: String? = null,
    val goalId: String? = null,
    val reconciled: Boolean = false,
    val importBatchId: String? = null,
    val attachmentPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "system_setting")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "budget", indices = [Index("month", "categoryId", unique = true)])
data class Budget(
    @PrimaryKey val id: String,
    val month: String, // YYYY-MM
    val categoryId: String,
    val plannedCents: Long,
    val rolloverEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "goal")
data class Goal(
    @PrimaryKey val id: String,
    val name: String,
    val targetCents: Long,
    val targetDate: LocalDate? = null,
    val linkedAccountId: String? = null,
    val color: String,
    val icon: String,
    val status: String = "ACTIVE", // ACTIVE, ACHIEVED, ARCHIVED
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "goal_contribution")
data class GoalContribution(
    @PrimaryKey val id: String,
    val goalId: String,
    val amountCents: Long,
    val date: LocalDate,
    val note: String? = null,
    val transactionId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "recurrence", indices = [Index("accountId")])
data class Recurrence(
    @PrimaryKey val id: String,
    val type: String, // INCOME, EXPENSE
    val description: String,
    val amountCents: Long? = null, // null when variable amount
    val isVariable: Boolean = false,
    val accountId: String,
    val categoryId: String? = null,
    val payee: String? = null,
    val notes: String? = null,
    val frequency: String, // DAILY, WEEKLY, MONTHLY, YEARLY
    val interval: Int = 1,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

@Entity(tableName = "invoice")
data class Invoice(
    @PrimaryKey val id: String,
    val accountId: String,
    val referenceMonth: LocalDate,
    val closingDate: LocalDate,
    val dueDate: LocalDate,
    val status: String, // OPEN, CLOSED, PAID
    val totalCents: Long,
    val paidAmountCents: Long = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

/** Divisão de um lançamento por categoria (M7 aba Divisoes; UI completa na v1.1). */
@Entity(tableName = "transaction_split", indices = [Index("transactionId")])
data class TransactionSplit(
    @PrimaryKey val id: String,
    val transactionId: String,
    val categoryId: String,
    val amountCents: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

/** Regra de categorização automática (M7 aba Regras; motor na v1.1). */
@Entity(tableName = "rule")
data class Rule(
    @PrimaryKey val id: String,
    val matchField: String, // DESCRIPTION, PAYEE
    val matchType: String, // CONTAINS, STARTS_WITH, REGEX
    val pattern: String,
    val setCategoryId: String? = null,
    val setPayee: String? = null,
    val setTags: String? = null,
    val priority: Int = 0,
    val active: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)

/** Resumo de um mês fechado (M7 aba Arquivo; ritual de fechamento no M5). */
@Entity(tableName = "month_closure")
data class MonthClosure(
    /** Chave estável: o próprio mês no formato AAAA-MM. */
    @PrimaryKey val id: String,
    val month: String, // AAAA-MM
    val incomeCents: Long,
    val expenseCents: Long,
    val balanceCents: Long,
    val closedAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false,
)
