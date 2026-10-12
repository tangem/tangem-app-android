package com.tangem.datasource.local.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

class CleanupKeyMigration<T>(private val key: Preferences.Key<T>) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        // do nothing
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData.contains(key)

    override suspend fun migrate(currentData: Preferences): Preferences {
        return currentData.toMutablePreferences().apply { remove(key) }
    }
}