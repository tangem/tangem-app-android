package com.tangem.datasource.local.preferences.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences

/**
 * Creates a [DataMigration] that moves a single value from a legacy `SharedPreferences` file into the
 * preferences DataStore.
 *
 * The Android/analytics-dependent implementation lives in core:datasource. Modules that own only their
 * own persisted key names (e.g. store:datasource) contribute such migrations through this contract,
 * without depending on core:datasource.
 *
 * @param legacyPrefsName file name of the legacy `SharedPreferences`.
 * @param legacyKeyName key under which the value is stored in the legacy `SharedPreferences`.
 * @param keyName name the value should take in the DataStore.
 */
interface SharedPreferencesKeyMigrationFactory {

    fun create(legacyPrefsName: String, legacyKeyName: String, keyName: String): DataMigration<Preferences>

    /**
     * Creates a [DataMigration] that moves the given [keyNames] from a legacy `SharedPreferences` file
     * (keeping the same names) into the preferences DataStore.
     */
    fun createBulk(legacyPrefsName: String, keyNames: Set<String>): DataMigration<Preferences>
}