package com.tangem.datasource.local.preferences

import androidx.datastore.preferences.core.*

/**
 * All preferences keys that DataStore<Preferences> is stored.
 *
 * !!!IMPORTANT!!!
 * Before DELETING active key add a [com.tangem.datasource.local.preferences.migration.CleanupKeyMigration]
 * in [com.tangem.datasource.local.preferences.di.AppPreferencesStoreModule]
 *
[REDACTED_AUTHOR]
 */
object PreferencesKeys {

    val APP_LOGS_KEY by lazy { stringPreferencesKey(name = "app_logs") }

    val apiConfigsEnvironmentKey by lazy { stringPreferencesKey(name = "apiConfigsEnvironment") }

    val WAS_LOG_FILE_CLEARED by lazy { booleanPreferencesKey(name = "wasLogFileCleared") }

    val SEED_FIRST_NOTIFICATION_SHOW_TIME by lazy { longPreferencesKey("seedFirstNotificationTime") }
}