package com.tangem.datasource.local.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tangem.datasource.local.preferences.PreferencesDataStore.INSTANCE
import com.tangem.datasource.local.preferences.PreferencesKeys.APP_LOGS_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY
import com.tangem.datasource.local.preferences.PreferencesKeys.SHOULD_SHOW_RING_PROMO_KEY
import com.tangem.datasource.local.preferences.utils.CleanupKeyMigration
import com.tangem.datasource.local.preferences.utils.SharedPreferencesKeyMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Application preferences data store 'DataStore<Preferences>'.
 * Implements the singleton pattern [INSTANCE] under the hood.
 *
[REDACTED_AUTHOR]
 */
internal object PreferencesDataStore {

    private const val PREFERENCES_FILE_NAME = "TAP_PREFS"
    private const val LEGACY_TAP_PREFS_FILE_NAME = "tapPrefs"
    private const val LEGACY_DEFAULT_KEY_NAME = "key"

    private var INSTANCE: DataStore<Preferences>? = null

    fun getInstance(context: Context, dispatcher: CoroutineContext): DataStore<Preferences> {
        return INSTANCE ?: create(context, dispatcher).also { INSTANCE = it }
    }

    private fun create(context: Context, dispatcher: CoroutineContext): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = createCorruptionHandler(),
            migrations = createMigrations(context = context),
            scope = CoroutineScope(context = dispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(name = PREFERENCES_FILE_NAME) },
        )
    }

    private fun createCorruptionHandler(): ReplaceFileCorruptionHandler<Preferences> {
        return ReplaceFileCorruptionHandler(
            produceNewData = {
                Timber.w(it)
                emptyPreferences()
            },
        )
    }

    private fun createMigrations(context: Context): List<DataMigration<Preferences>> {
        return listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = LEGACY_TAP_PREFS_FILE_NAME,
                keysToMigrate = getTapPrefKeysToMigrate(),
            ),
            SharedPreferencesKeyMigration(
                context = context,
                legacyPrefsName = "app_theme",
                legacyKeyName = LEGACY_DEFAULT_KEY_NAME,
                keyName = PreferencesKeys.APP_THEME_MODE_KEY.name,
            ),
            SharedPreferencesKeyMigration(
                context = context,
                legacyPrefsName = "selected_app_currency",
                legacyKeyName = LEGACY_DEFAULT_KEY_NAME,
                keyName = PreferencesKeys.SELECTED_APP_CURRENCY_KEY.name,
            ),
            SharedPreferencesKeyMigration(
                context = context,
                legacyPrefsName = "balance_hiding_settings",
                legacyKeyName = LEGACY_DEFAULT_KEY_NAME,
                keyName = PreferencesKeys.BALANCE_HIDING_SETTINGS_KEY.name,
            ),
            CleanupKeyMigration(key = APP_LOGS_KEY),
            CleanupKeyMigration(key = IS_WALLET_SWAP_PROMO_OKX_SHOW_KEY),
            CleanupKeyMigration(key = IS_TOKEN_SWAP_PROMO_OKX_SHOW_KEY),
            CleanupKeyMigration(key = SHOULD_SHOW_RING_PROMO_KEY),
        )
    }
}