package com.example.lev.data.repository

import com.example.lev.data.local.AppDatabase
import com.example.lev.domain.RowParseResult
import com.example.lev.domain.SheetRow
import com.example.lev.domain.SheetSchema
import com.example.lev.domain.SheetTab
import com.example.lev.domain.rowToAccount
import com.example.lev.domain.rowToBudget
import com.example.lev.domain.rowToCategory
import com.example.lev.domain.rowToClosure
import com.example.lev.domain.rowToContribution
import com.example.lev.domain.rowToGoal
import com.example.lev.domain.rowToInvoice
import com.example.lev.domain.rowToRecurrence
import com.example.lev.domain.rowToRule
import com.example.lev.domain.rowToSetting
import com.example.lev.domain.rowToSplit
import com.example.lev.domain.rowToTransaction
import com.example.lev.domain.splitIdOf
import com.example.lev.domain.toSheetRow

internal data class DirtyRow(val id: String, val row: SheetRow)

internal data class TabSync<T : Any>(
    val tab: SheetTab,
    val dirty: suspend () -> List<DirtyRow>,
    val all: suspend () -> List<T>,
    val allIds: suspend () -> List<String>,
    val toRow: (T) -> SheetRow,
    val fromRow: (SheetRow, Int) -> RowParseResult<T>,
    val idOf: (T) -> String,
    val updatedAtOf: (T) -> Long,
    val markSynced: suspend (List<String>, Long) -> Unit,
    val upsert: suspend (List<T>) -> Unit,
    val clear: suspend () -> Unit,
    val count: suspend () -> Int,
    val purge: suspend (Long) -> Unit,
)

private fun <T : Any> tabOf(
    tab: SheetTab,
    dirty: suspend () -> List<DirtyRow>,
    all: suspend () -> List<T>,
    toRow: (T) -> SheetRow,
    fromRow: (SheetRow, Int) -> RowParseResult<T>,
    idOf: (T) -> String,
    updatedAtOf: (T) -> Long,
    markSynced: suspend (List<String>, Long) -> Unit,
    upsert: suspend (List<T>) -> Unit,
    clear: suspend () -> Unit,
    count: suspend () -> Int,
    purge: suspend (Long) -> Unit,
): TabSync<T> =
    TabSync(
        tab, dirty, all, { all().map(idOf) }, toRow, fromRow,
        idOf, updatedAtOf, markSynced, upsert, clear, count, purge,
    )

/** Abas de dados sincronizáveis, na ordem de push/pull (pais antes de filhos). */
internal suspend fun buildSyncTabs(db: AppDatabase): List<TabSync<*>> {
    val accountNames = db.accountDao().getAllForSync().associate { it.id to it.name }
    val categoryNames = db.categoryDao().getAllForSync().associate { it.id to it.name }
    return listOf(
        accountsTab(db),
        categoriesTab(db),
        transactionsTab(db, accountNames, categoryNames),
        splitsTab(db),
        invoicesTab(db),
        budgetsTab(db),
        goalsTab(db),
        contributionsTab(db),
        recurrencesTab(db),
        rulesTab(db),
        archiveTab(db),
        configTab(db),
    )
}

private fun accountsTab(db: AppDatabase): TabSync<com.example.lev.data.model.Account> {
    val dao = db.accountDao()
    return tabOf(
        tab = SheetSchema.ACCOUNTS,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToAccount(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun categoriesTab(db: AppDatabase): TabSync<com.example.lev.data.model.Category> {
    val dao = db.categoryDao()
    return tabOf(
        tab = SheetSchema.CATEGORIES,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToCategory(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun transactionsTab(
    db: AppDatabase,
    accountNames: Map<String, String>,
    categoryNames: Map<String, String>,
): TabSync<com.example.lev.data.model.Transaction> {
    val dao = db.transactionDao()
    val toRow: (com.example.lev.data.model.Transaction) -> SheetRow = {
        it.toSheetRow(
            { id -> accountNames[id] ?: "" },
            { id -> categoryNames[id] ?: "" },
        )
    }
    return tabOf(
        tab = SheetSchema.TRANSACTIONS,
        dirty = { dao.getDirty().map { DirtyRow(it.id, toRow(it)) } },
        all = { dao.getAllForSync() },
        toRow = toRow,
        fromRow = { row, line -> rowToTransaction(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun splitsTab(db: AppDatabase): TabSync<com.example.lev.data.model.TransactionSplit> {
    val dao = db.transactionSplitDao()
    return tabOf(
        tab = SheetSchema.SPLITS,
        dirty = {
            dao.getDirty().map {
                val key =
                    splitIdOf(it.transactionId, it.categoryId, it.amountCents, it.note ?: "")
                DirtyRow(key, it.toSheetRow())
            }
        },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToSplit(row, line, ::splitIdOf) },
        idOf = {
            splitIdOf(it.transactionId, it.categoryId, it.amountCents, it.note ?: "")
        },
        updatedAtOf = { it.updatedAt },
        markSynced = { _, ts ->
            // Sem coluna ID: o push é sempre integral (replace).
            val stillDirty = dao.getDirty().map { it.id }
            if (stillDirty.isNotEmpty()) dao.markSynced(stillDirty, ts)
        },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun invoicesTab(db: AppDatabase): TabSync<com.example.lev.data.model.Invoice> {
    val dao = db.invoiceDao()
    return tabOf(
        tab = SheetSchema.INVOICES,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToInvoice(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun budgetsTab(db: AppDatabase): TabSync<com.example.lev.data.model.Budget> {
    val dao = db.budgetDao()
    return tabOf(
        tab = SheetSchema.BUDGETS,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToBudget(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun goalsTab(db: AppDatabase): TabSync<com.example.lev.data.model.Goal> {
    val dao = db.goalDao()
    return tabOf(
        tab = SheetSchema.GOALS,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToGoal(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun contributionsTab(db: AppDatabase): TabSync<com.example.lev.data.model.GoalContribution> {
    val dao = db.goalContributionDao()
    return tabOf(
        tab = SheetSchema.GOAL_CONTRIBUTIONS,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToContribution(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun recurrencesTab(db: AppDatabase): TabSync<com.example.lev.data.model.Recurrence> {
    val dao = db.recurrenceDao()
    return tabOf(
        tab = SheetSchema.RECURRENCES,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToRecurrence(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun rulesTab(db: AppDatabase): TabSync<com.example.lev.data.model.Rule> {
    val dao = db.ruleDao()
    return tabOf(
        tab = SheetSchema.RULES,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToRule(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun archiveTab(db: AppDatabase): TabSync<com.example.lev.data.model.MonthClosure> {
    val dao = db.monthClosureDao()
    return tabOf(
        tab = SheetSchema.ARCHIVE,
        dirty = { dao.getDirty().map { DirtyRow(it.id, it.toSheetRow()) } },
        all = { dao.getAllForSync() },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToClosure(row, line) },
        idOf = { it.id },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = { dao.upsertAll(it) },
        clear = { dao.clearAll() },
        count = { dao.countAll() },
        purge = { dao.purgeTombstones(it) },
    )
}

private fun configTab(db: AppDatabase): TabSync<com.example.lev.data.model.SystemSetting> {
    val dao = db.systemSettingDao()
    return tabOf(
        tab = SheetSchema.CONFIG,
        dirty = {
            dao.getDirty()
                .filter { it.key in SyncRepository.SYNCED_CONFIG_KEYS }
                .map { DirtyRow(it.key, it.toSheetRow()) }
        },
        all = {
            dao.getAllForSync().filter { it.key in SyncRepository.SYNCED_CONFIG_KEYS }
        },
        toRow = { it.toSheetRow() },
        fromRow = { row, line -> rowToSetting(row, line) },
        idOf = { it.key },
        updatedAtOf = { it.updatedAt },
        markSynced = { ids, ts -> dao.markSynced(ids, ts) },
        upsert = {
            dao.upsertAll(
                it.filter { s ->
                    s.key in SyncRepository.SYNCED_CONFIG_KEYS &&
                        (s.key != SyncRepository.SCHEMA_VERSION_KEY || isSupportedSchema(s.value))
                },
            )
        },
        // Config nunca é apagada no replace: chaves locais sobrevivem.
        clear = { },
        count = { dao.getAllForSync().count { it.key in SyncRepository.SYNCED_CONFIG_KEYS } },
        purge = { },
    )
}

/** Rejeita schema_version de um app mais novo (não sabemos aplicar). */
private fun isSupportedSchema(value: String): Boolean = (value.toIntOrNull() ?: 0) <= SyncRepository.DB_SCHEMA_VERSION
