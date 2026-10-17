package com.example.lev.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from v1 (accounts/categories/transactions/settings/invoices only)
 * to v2 (budgets, goals, goal contributions and recurrences).
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `budget` (
                    `id` TEXT NOT NULL,
                    `month` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `plannedCents` INTEGER NOT NULL,
                    `rolloverEnabled` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_budget_month_categoryId` ON `budget` (`month`, `categoryId`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `goal` (
                    `id` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `targetCents` INTEGER NOT NULL,
                    `targetDate` INTEGER,
                    `linkedAccountId` TEXT,
                    `color` TEXT NOT NULL,
                    `icon` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `goal_contribution` (
                    `id` TEXT NOT NULL,
                    `goalId` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `date` INTEGER NOT NULL,
                    `note` TEXT,
                    `transactionId` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recurrence` (
                    `id` TEXT NOT NULL,
                    `type` TEXT NOT NULL,
                    `description` TEXT NOT NULL,
                    `amountCents` INTEGER,
                    `isVariable` INTEGER NOT NULL,
                    `accountId` TEXT NOT NULL,
                    `categoryId` TEXT,
                    `payee` TEXT,
                    `notes` TEXT,
                    `frequency` TEXT NOT NULL,
                    `interval` INTEGER NOT NULL,
                    `dayOfMonth` INTEGER,
                    `dayOfWeek` INTEGER,
                    `startDate` INTEGER NOT NULL,
                    `endDate` INTEGER,
                    `maxOccurrences` INTEGER,
                    `active` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_accountId` ON `recurrence` (`accountId`)")
        }
    }

/**
 * Migration from v2 to v3 (M7 sync): divisões de lançamentos, regras de
 * categorização e fechamentos de mês, com colunas de sincronização.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `transaction_split` (
                    `id` TEXT NOT NULL,
                    `transactionId` TEXT NOT NULL,
                    `categoryId` TEXT NOT NULL,
                    `amountCents` INTEGER NOT NULL,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transaction_split_transactionId` " +
                    "ON `transaction_split` (`transactionId`)",
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `rule` (
                    `id` TEXT NOT NULL,
                    `matchField` TEXT NOT NULL,
                    `matchType` TEXT NOT NULL,
                    `pattern` TEXT NOT NULL,
                    `setCategoryId` TEXT,
                    `setPayee` TEXT,
                    `setTags` TEXT,
                    `priority` INTEGER NOT NULL,
                    `active` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `month_closure` (
                    `id` TEXT NOT NULL,
                    `month` TEXT NOT NULL,
                    `incomeCents` INTEGER NOT NULL,
                    `expenseCents` INTEGER NOT NULL,
                    `balanceCents` INTEGER NOT NULL,
                    `closedAt` INTEGER NOT NULL,
                    `note` TEXT,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `syncedAt` INTEGER,
                    `deleted` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
        }
    }
