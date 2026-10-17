package com.example.lev.data.repository

import com.example.lev.data.local.SystemSettingDao
import com.example.lev.data.model.SystemSetting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingDao: SystemSettingDao,
) {
    suspend fun getSetting(key: String): String? {
        return settingDao.getSetting(key)?.value
    }

    fun observeSetting(key: String): Flow<String?> = settingDao.observeSetting(key).map { it?.value }

    suspend fun saveSetting(
        key: String,
        value: String,
    ) {
        val setting =
            SystemSetting(
                key = key,
                value = value,
                updatedAt = System.currentTimeMillis(),
            )
        settingDao.insert(setting)
    }

    suspend fun isOnboardingComplete(): Boolean {
        return getSetting("onboarding_complete") == "true"
    }

    suspend fun setOnboardingComplete() {
        saveSetting("onboarding_complete", "true")
    }
}
