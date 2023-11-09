package com.tangem.datasource.local.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.tangem.datasource.local.preferences.PreferencesDataStore.INSTANCE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * Application preferences data store 'DataStore<Preferences>'.
 * Implements the singleton pattern [INSTANCE] under the hood.
 *
* [REDACTED_AUTHOR]
 */
internal object PreferencesDataStore {

    private const val PREFERENCES_FILE_NAME = "TAP_PREFS"

    private var INSTANCE: DataStore<Preferences>? = null

    fun getInstance(context: Context, dispatcher: CoroutineContext): DataStore<Preferences> {
        return INSTANCE ?: create(context, dispatcher).also { INSTANCE = it }
    }

    private fun create(context: Context, dispatcher: CoroutineContext): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = createCorruptionHandler(),
            migrations = createMigrations(),
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

    private fun createMigrations(): List<DataMigration<Preferences>> {
        return listOf()
    }
}
