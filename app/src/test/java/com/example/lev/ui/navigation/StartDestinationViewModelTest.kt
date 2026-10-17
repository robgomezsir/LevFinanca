package com.example.lev.ui.navigation

import com.example.lev.data.local.SystemSettingDao
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * O app não pode voltar ao onboarding depois de concluído: cada passagem recria a primeira
 * conta e as categorias padrão.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StartDestinationViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sem onboarding concluido abre no onboarding`() =
        runTest(dispatcher) {
            val viewModel = StartDestinationViewModel(SettingsRepository(FakeSystemSettingDao()))
            advanceUntilIdle()

            assertEquals(Screen.Onboarding.route, viewModel.startDestination.value)
        }

    @Test
    fun `onboarding concluido abre direto no main`() =
        runTest(dispatcher) {
            val dao = FakeSystemSettingDao(mutableMapOf("onboarding_complete" to "true"))
            val viewModel = StartDestinationViewModel(SettingsRepository(dao))
            advanceUntilIdle()

            assertEquals(Screen.Main.route, viewModel.startDestination.value)
        }

    @Test
    fun `flag diferente de true nao conta como concluido`() =
        runTest(dispatcher) {
            val dao = FakeSystemSettingDao(mutableMapOf("onboarding_complete" to "false"))
            val viewModel = StartDestinationViewModel(SettingsRepository(dao))
            advanceUntilIdle()

            assertEquals(Screen.Onboarding.route, viewModel.startDestination.value)
        }

    private class FakeSystemSettingDao(
        private val settings: MutableMap<String, String> = mutableMapOf(),
    ) : SystemSettingDao {
        override suspend fun getSetting(key: String): SystemSetting? =
            settings[key]?.let { SystemSetting(key = key, value = it, updatedAt = 0L) }

        override fun observeSetting(key: String): Flow<SystemSetting?> =
            flowOf(settings[key]?.let { SystemSetting(key = key, value = it, updatedAt = 0L) })

        override suspend fun insert(setting: SystemSetting) {
            settings[setting.key] = setting.value
        }

        override suspend fun getAllForSync(): List<SystemSetting> =
            settings.map { (k, v) -> SystemSetting(key = k, value = v, updatedAt = 0L) }

        override suspend fun getDirty(): List<SystemSetting> = emptyList()

        override suspend fun markSynced(
            keys: List<String>,
            syncedAt: Long,
        ) = Unit

        override suspend fun upsertAll(rows: List<SystemSetting>) {
            rows.forEach { settings[it.key] = it.value }
        }

        override suspend fun countAll(): Int = settings.size

        override suspend fun clearAll() {
            settings.clear()
        }
    }
}
