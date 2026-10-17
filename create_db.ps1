$entitiesPath = "app\src\main\java\com\example\lev\data\model"
$localPath = "app\src\main\java\com\example\lev\data\local"

New-Item -ItemType Directory -Force -Path $entitiesPath | Out-Null
New-Item -ItemType Directory -Force -Path $localPath | Out-Null

$entities = @"
package com.example.lev.data.model

import androidx.room.Entity
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
    val paymentAccountId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false
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
    val deleted: Boolean = false
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
    val deleted: Boolean = false
)

@Entity(tableName = "system_setting")
data class SystemSetting(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long,
    val syncedAt: Long? = null,
    val deleted: Boolean = false
)
"@
Set-Content -Path "$entitiesPath\Entities.kt" -Value $entities -Encoding UTF8

$typeConverters = @"
package com.example.lev.data.local

import androidx.room.TypeConverter
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }
}
"@
Set-Content -Path "$localPath\Converters.kt" -Value $typeConverters -Encoding UTF8

$daos = @"
package com.example.lev.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account WHERE deleted = 0 ORDER BY sortOrder ASC")
    fun getAllActive(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE deleted = 0 ORDER BY sortOrder ASC")
    fun getAllActive(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM `transaction` WHERE deleted = 0 ORDER BY date DESC")
    fun getAllActive(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)
}

@Dao
interface SystemSettingDao {
    @Query("SELECT * FROM system_setting WHERE `key` = :key AND deleted = 0 LIMIT 1")
    suspend fun getSetting(key: String): SystemSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: SystemSetting)
}
"@
Set-Content -Path "$localPath\Daos.kt" -Value $daos -Encoding UTF8

$database = @"
package com.example.lev.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lev.data.model.Account
import com.example.lev.data.model.Category
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.model.Transaction

@Database(
    entities = [Account::class, Category::class, Transaction::class, SystemSetting::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun systemSettingDao(): SystemSettingDao
}
"@
Set-Content -Path "$localPath\AppDatabase.kt" -Value $database -Encoding UTF8
