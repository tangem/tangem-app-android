package com.tangem.datasource.local.preferences.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

internal class CleanupKeyMigration<T>(private val key: Preferences.Key<T>) : DataMigration<Preferences> {

    override suspend fun cleanUp() {
        // do nothing
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData.contains(key)

    override suspend fun migrate(currentData: Preferences): Preferences {
        currentData.toMutablePreferences().remove(key)
        return currentData
    }
}