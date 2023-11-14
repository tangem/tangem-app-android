package com.tangem.datasource.local.preferences

/**
 * All preferences keys that DataStore<Preferences> is stored.
 *
[REDACTED_AUTHOR]
 */
object PreferencesKeys

/** Preferences keys set that should be migrated from "PreferencesDataSource" to a new DataStore<Preferences> */
internal fun getTapPrefKeysToMigrate(): Set<String> {
    return setOf()
}