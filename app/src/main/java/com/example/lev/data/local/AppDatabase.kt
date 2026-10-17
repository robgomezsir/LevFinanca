package com.example.lev.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lev.data.model.Account
import com.example.lev.data.model.Budget
import com.example.lev.data.model.Category
import com.example.lev.data.model.Goal
import com.example.lev.data.model.GoalContribution
import com.example.lev.data.model.Invoice
import com.example.lev.data.model.MonthClosure
import com.example.lev.data.model.Recurrence
import com.example.lev.data.model.Rule
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.model.Transaction
import com.example.lev.data.model.TransactionSplit

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        SystemSetting::class,
        Invoice::class,
        Budget::class,
        Goal::class,
        GoalContribution::class,
        Recurrence::class,
        TransactionSplit::class,
        Rule::class,
        MonthClosure::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun systemSettingDao(): SystemSettingDao

    abstract fun invoiceDao(): InvoiceDao

    abstract fun budgetDao(): BudgetDao

    abstract fun goalDao(): GoalDao

    abstract fun goalContributionDao(): GoalContributionDao

    abstract fun recurrenceDao(): RecurrenceDao

    abstract fun transactionSplitDao(): TransactionSplitDao

    abstract fun ruleDao(): RuleDao

    abstract fun monthClosureDao(): MonthClosureDao
}
