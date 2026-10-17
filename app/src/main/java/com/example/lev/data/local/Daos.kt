package com.example.lev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
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
import kotlinx.coroutines.flow.Flow

/**
 * Métodos de sincronização (M7), presentes em todos os DAOs sincronizáveis:
 * - [getAllForSync]: tudo, inclusive tombstones (push integral e merge).
 * - [getDirty]: linhas com updatedAt > syncedAt (push incremental).
 * - [markSynced]: carimba syncedAt após push confirmado.
 * - [upsertAll]: aplica linhas vindas da planilha (pull/merge).
 * - [purgeTombstones]: remoção definitiva de tombstones antigos (compact, 90 dias).
 * - [clearAll]: usado apenas no modo Substituir/Baixar, após backup.
 */
@Dao
interface AccountDao {
    @Query("SELECT * FROM account WHERE deleted = 0 ORDER BY sortOrder ASC")
    fun getAllActive(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Update
    suspend fun update(account: Account)

    @Query("UPDATE account SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM account")
    suspend fun getAllForSync(): List<Account>

    @Query("SELECT * FROM account WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Account>

    @Query("UPDATE account SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Account>)

    @Query("DELETE FROM account WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM account")
    suspend fun countAll(): Int

    @Query("DELETE FROM account")
    suspend fun clearAll()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE deleted = 0 ORDER BY sortOrder ASC")
    fun getAllActive(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)

    @Update
    suspend fun update(category: Category)

    @Query("UPDATE category SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM category")
    suspend fun getAllForSync(): List<Category>

    @Query("SELECT * FROM category WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Category>

    @Query("UPDATE category SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Category>)

    @Query("DELETE FROM category WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM category")
    suspend fun countAll(): Int

    @Query("DELETE FROM category")
    suspend fun clearAll()
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transaction` WHERE deleted = 0 ORDER BY date DESC")
    fun getAllActive(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Query("UPDATE `transaction` SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM `transaction`")
    suspend fun getAllForSync(): List<Transaction>

    @Query("SELECT * FROM `transaction` WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Transaction>

    @Query("UPDATE `transaction` SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Transaction>)

    @Query("DELETE FROM `transaction` WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM `transaction`")
    suspend fun countAll(): Int

    @Query("DELETE FROM `transaction`")
    suspend fun clearAll()
}

@Dao
interface SystemSettingDao {
    @Query("SELECT * FROM system_setting WHERE key = :key AND deleted = 0 LIMIT 1")
    suspend fun getSetting(key: String): SystemSetting?

    @Query("SELECT * FROM system_setting WHERE key = :key AND deleted = 0 LIMIT 1")
    fun observeSetting(key: String): Flow<SystemSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SystemSetting)

    @Query("SELECT * FROM system_setting")
    suspend fun getAllForSync(): List<SystemSetting>

    @Query("SELECT * FROM system_setting WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<SystemSetting>

    @Query("UPDATE system_setting SET syncedAt = :syncedAt WHERE key IN (:keys)")
    suspend fun markSynced(
        keys: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<SystemSetting>)

    @Query("SELECT COUNT(*) FROM system_setting")
    suspend fun countAll(): Int

    @Query("DELETE FROM system_setting")
    suspend fun clearAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoice WHERE deleted = 0 AND accountId = :accountId ORDER BY referenceMonth DESC")
    fun getInvoicesForAccount(accountId: String): Flow<List<Invoice>>

    @Query("SELECT * FROM invoice WHERE deleted = 0")
    fun getAll(): Flow<List<Invoice>>

    @Query("SELECT * FROM invoice WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Invoice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: Invoice)

    @Query("SELECT * FROM invoice")
    suspend fun getAllForSync(): List<Invoice>

    @Query("SELECT * FROM invoice WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Invoice>

    @Query("UPDATE invoice SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Invoice>)

    @Query("DELETE FROM invoice WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM invoice")
    suspend fun countAll(): Int

    @Query("DELETE FROM invoice")
    suspend fun clearAll()
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budget WHERE deleted = 0 ORDER BY categoryId ASC")
    fun getAllActive(): Flow<List<Budget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: Budget)

    @Query("DELETE FROM budget WHERE month = :month AND categoryId = :categoryId")
    suspend fun deleteByMonthCategory(
        month: String,
        categoryId: String,
    )

    @Query("SELECT * FROM budget")
    suspend fun getAllForSync(): List<Budget>

    @Query("SELECT * FROM budget WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Budget>

    @Query("UPDATE budget SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Budget>)

    @Query("DELETE FROM budget WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM budget")
    suspend fun countAll(): Int

    @Query("DELETE FROM budget")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goal WHERE deleted = 0 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: Goal)

    @Update
    suspend fun update(goal: Goal)

    @Query("UPDATE goal SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM goal")
    suspend fun getAllForSync(): List<Goal>

    @Query("SELECT * FROM goal WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Goal>

    @Query("UPDATE goal SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Goal>)

    @Query("DELETE FROM goal WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM goal")
    suspend fun countAll(): Int

    @Query("DELETE FROM goal")
    suspend fun clearAll()
}

@Dao
interface GoalContributionDao {
    @Query("SELECT * FROM goal_contribution WHERE deleted = 0 ORDER BY date DESC")
    fun getAllActive(): Flow<List<GoalContribution>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: GoalContribution)

    @Query("UPDATE goal_contribution SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM goal_contribution")
    suspend fun getAllForSync(): List<GoalContribution>

    @Query("SELECT * FROM goal_contribution WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<GoalContribution>

    @Query("UPDATE goal_contribution SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<GoalContribution>)

    @Query("DELETE FROM goal_contribution WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM goal_contribution")
    suspend fun countAll(): Int

    @Query("DELETE FROM goal_contribution")
    suspend fun clearAll()
}

@Dao
interface RecurrenceDao {
    @Query("SELECT * FROM recurrence WHERE deleted = 0 ORDER BY startDate DESC")
    fun getAllActive(): Flow<List<Recurrence>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recurrence: Recurrence)

    @Update
    suspend fun update(recurrence: Recurrence)

    @Query("UPDATE recurrence SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM recurrence")
    suspend fun getAllForSync(): List<Recurrence>

    @Query("SELECT * FROM recurrence WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Recurrence>

    @Query("UPDATE recurrence SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Recurrence>)

    @Query("DELETE FROM recurrence WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM recurrence")
    suspend fun countAll(): Int

    @Query("DELETE FROM recurrence")
    suspend fun clearAll()
}

@Dao
interface TransactionSplitDao {
    @Query("SELECT * FROM transaction_split WHERE transactionId = :transactionId AND deleted = 0")
    fun getForTransaction(transactionId: String): Flow<List<TransactionSplit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(split: TransactionSplit)

    @Query("UPDATE transaction_split SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM transaction_split")
    suspend fun getAllForSync(): List<TransactionSplit>

    @Query("SELECT * FROM transaction_split WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<TransactionSplit>

    @Query("UPDATE transaction_split SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<TransactionSplit>)

    @Query("DELETE FROM transaction_split WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM transaction_split")
    suspend fun countAll(): Int

    @Query("DELETE FROM transaction_split")
    suspend fun clearAll()
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM rule WHERE deleted = 0 ORDER BY priority DESC")
    fun getAllActive(): Flow<List<Rule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: Rule)

    @Update
    suspend fun update(rule: Rule)

    @Query("UPDATE rule SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM rule")
    suspend fun getAllForSync(): List<Rule>

    @Query("SELECT * FROM rule WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<Rule>

    @Query("UPDATE rule SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<Rule>)

    @Query("DELETE FROM rule WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM rule")
    suspend fun countAll(): Int

    @Query("DELETE FROM rule")
    suspend fun clearAll()
}

@Dao
interface MonthClosureDao {
    @Query("SELECT * FROM month_closure WHERE deleted = 0 ORDER BY month DESC")
    fun getAllActive(): Flow<List<MonthClosure>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(closure: MonthClosure)

    @Query("UPDATE month_closure SET deleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(
        id: String,
        updatedAt: Long,
    )

    @Query("SELECT * FROM month_closure")
    suspend fun getAllForSync(): List<MonthClosure>

    @Query("SELECT * FROM month_closure WHERE updatedAt > COALESCE(syncedAt, 0)")
    suspend fun getDirty(): List<MonthClosure>

    @Query("UPDATE month_closure SET syncedAt = :syncedAt WHERE id IN (:ids)")
    suspend fun markSynced(
        ids: List<String>,
        syncedAt: Long,
    )

    @Upsert
    suspend fun upsertAll(rows: List<MonthClosure>)

    @Query("DELETE FROM month_closure WHERE deleted = 1 AND updatedAt < :before")
    suspend fun purgeTombstones(before: Long)

    @Query("SELECT COUNT(*) FROM month_closure")
    suspend fun countAll(): Int

    @Query("DELETE FROM month_closure")
    suspend fun clearAll()
}
