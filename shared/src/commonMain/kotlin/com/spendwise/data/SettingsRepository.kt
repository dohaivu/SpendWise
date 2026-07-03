package com.spendwise.data

import com.spendwise.domain.UserSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<UserSettings>
    suspend fun saveSettings(settings: UserSettings)
}

class DataStoreSettingsRepository(
    private val dataStore: AppDataStore
) : SettingsRepository {
    override val settings: Flow<UserSettings> = dataStore.settings

    override suspend fun saveSettings(settings: UserSettings) {
        dataStore.saveSettings(settings)
    }
}
