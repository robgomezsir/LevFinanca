package com.example.lev.ui.theme

import com.example.lev.data.local.SystemSettingDao
import com.example.lev.data.model.SystemSetting
import com.example.lev.data.repository.SettingsRepository
import com.example.lev.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
 * O sistema de Temas (claro/escuro/sistema) e Aparência (sólido/vidro) é persistido em
 * SystemSetting: sem persistência o app volta ao padrão a cada abertura, e sem o
 * ThemeViewModel lendo as mesmas chaves a mudança feita em Configurações nunca chega ao
 * MainActivity. Estes testes travam o parse, a composição do estado e a gravação.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSystemTest {
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
    fun `chave desconhecida ou ausente cai no padrao automatico e solido`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("banana"))
        assertEquals(ThemeMode.LIGHT, ThemeMode.fromKey("light"))
        assertEquals(ThemeMode.DARK, ThemeMode.fromKey("dark"))
        assertEquals(Appearance.SOLID, Appearance.fromKey(null))
        assertEquals(Appearance.SOLID, Appearance.fromKey(""))
        assertEquals(Appearance.SOLID, Appearance.fromKey("banana"))
        assertEquals(Appearance.GLASS, Appearance.fromKey("glass"))
    }

    @Test
    fun `ThemeViewModel reflete tema escuro, vidro e cor dinamica persistidos`() =
        runTest(dispatcher) {
            val repository =
                SettingsRepository(
                    FakeSystemSettingDao(
                        mutableMapOf(
                            THEME_MODE_KEY to "dark",
                            APPEARANCE_KEY to "glass",
                            DYNAMIC_COLOR_KEY to "true",
                        ),
                    ),
                )
            val viewModel = ThemeViewModel(repository)
            val collect = launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.mode)
            assertEquals(Appearance.GLASS, viewModel.uiState.value.appearance)
            assertEquals(true, viewModel.uiState.value.dynamicColor)
            collect.cancel()
        }

    @Test
    fun `ThemeViewModel sem nada persistido fica automatico e solido`() =
        runTest(dispatcher) {
            val viewModel = ThemeViewModel(SettingsRepository(FakeSystemSettingDao()))
            val collect = launch { viewModel.uiState.collect { } }
            advanceUntilIdle()

            assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.mode)
            assertEquals(Appearance.SOLID, viewModel.uiState.value.appearance)
            assertEquals(false, viewModel.uiState.value.dynamicColor)
            collect.cancel()
        }

    @Test
    fun `setThemeMode e setAppearance gravam as chaves que o ThemeViewModel lê`() =
        runTest(dispatcher) {
            val dao = FakeSystemSettingDao()
            val repository = SettingsRepository(dao)
            val viewModel = SettingsViewModel(repository)
            advanceUntilIdle()

            viewModel.setThemeMode(ThemeMode.DARK)
            viewModel.setAppearance(Appearance.GLASS)
            viewModel.setDynamicColor(true)
            advanceUntilIdle()

            assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
            assertEquals(Appearance.GLASS, viewModel.uiState.value.appearance)
            assertEquals(true, viewModel.uiState.value.dynamicColor)
            // As mesmas chaves que o ThemeViewModel (usado pelo MainActivity) observa.
            assertEquals("dark", repository.getSetting(THEME_MODE_KEY))
            assertEquals("glass", repository.getSetting(APPEARANCE_KEY))
            assertEquals("true", repository.getSetting(DYNAMIC_COLOR_KEY))

            val theme = ThemeViewModel(repository)
            val collect = launch { theme.uiState.collect { } }
            advanceUntilIdle()
            assertEquals(ThemeMode.DARK, theme.uiState.value.mode)
            assertEquals(Appearance.GLASS, theme.uiState.value.appearance)
            collect.cancel()
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
