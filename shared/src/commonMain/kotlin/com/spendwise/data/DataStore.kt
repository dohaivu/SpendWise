package com.spendwise.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.spendwise.domain.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

class AppDataStore(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<UserSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            UserSettings(
                baseCurrencyCode = prefs[KEY_BASE_CURRENCY] ?: "USD",
                languageCode = prefs[KEY_LANGUAGE] ?: "en",
                themeModeCode = prefs[KEY_THEME_MODE] ?: "system",
                colorSchemeModeCode = prefs[KEY_COLOR_SCHEME] ?: "sunset",
                backupFolderUri = prefs[KEY_BACKUP_FOLDER_URI],
                backupFolderName = prefs[KEY_BACKUP_FOLDER_NAME],
                lastBackupAtMillis = prefs[KEY_LAST_BACKUP_AT]
            )
        }

    suspend fun saveSettings(settings: UserSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_BASE_CURRENCY] = settings.baseCurrencyCode
            prefs[KEY_LANGUAGE] = settings.languageCode
            prefs[KEY_THEME_MODE] = settings.themeModeCode
            prefs[KEY_COLOR_SCHEME] = settings.colorSchemeModeCode
            if (settings.backupFolderUri != null) {
                prefs[KEY_BACKUP_FOLDER_URI] = settings.backupFolderUri
            } else {
                prefs.remove(KEY_BACKUP_FOLDER_URI)
            }
            if (settings.backupFolderName != null) {
                prefs[KEY_BACKUP_FOLDER_NAME] = settings.backupFolderName
            } else {
                prefs.remove(KEY_BACKUP_FOLDER_NAME)
            }
            if (settings.lastBackupAtMillis != null) {
                prefs[KEY_LAST_BACKUP_AT] = settings.lastBackupAtMillis
            } else {
                prefs.remove(KEY_LAST_BACKUP_AT)
            }
        }
    }

    private companion object {
        val KEY_BASE_CURRENCY = stringPreferencesKey("base_currency")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_COLOR_SCHEME = stringPreferencesKey("color_scheme_mode")
        val KEY_BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val KEY_BACKUP_FOLDER_NAME = stringPreferencesKey("backup_folder_name")
        val KEY_LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
    }
}

internal const val dataStoreFileName = "settings.preferences_pb"

fun getPreferencesDataStore(path: String) = PreferenceDataStoreFactory.createWithPath {
    path.toPath()
}

expect fun createPreferencesDataStore(): DataStore<Preferences>
