package com.example.lev

import android.content.Context
import androidx.room.Room
import com.example.lev.data.local.AppDatabase
import com.example.lev.data.local.MIGRATION_1_2
import com.example.lev.data.local.MIGRATION_2_3
import com.example.lev.data.local.SyncSecretStore
import com.example.lev.data.remote.SheetsClient
import com.example.lev.data.remote.SheetsGateway
import com.example.lev.data.repository.FinanceRepository
import com.example.lev.data.repository.SettingsRepository
import com.example.lev.data.repository.SyncRepository

interface AppContainer {
    val database: AppDatabase
    val financeRepository: FinanceRepository
    val settingsRepository: SettingsRepository
    val syncSecretStore: SyncSecretStore
    val sheetsGateway: SheetsGateway
    val syncRepository: SyncRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        Room.databaseBuilder(context, AppDatabase::class.java, "lev_database")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    override val financeRepository: FinanceRepository by lazy {
        FinanceRepository(
            accountDao = database.accountDao(),
            categoryDao = database.categoryDao(),
            transactionDao = database.transactionDao(),
            invoiceDao = database.invoiceDao(),
            budgetDao = database.budgetDao(),
            goalDao = database.goalDao(),
            goalContributionDao = database.goalContributionDao(),
            recurrenceDao = database.recurrenceDao(),
            onLocalChange = { syncRepository.notifyLocalChange() },
        )
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(database.systemSettingDao())
    }

    override val syncSecretStore: SyncSecretStore by lazy {
        SyncSecretStore(context)
    }

    override val sheetsGateway: SheetsGateway by lazy {
        SheetsClient()
    }

    override val syncRepository: SyncRepository by lazy {
        SyncRepository(
            context = context,
            database = database,
            settingsRepository = settingsRepository,
            secretStore = syncSecretStore,
            gateway = sheetsGateway,
        )
    }
}
