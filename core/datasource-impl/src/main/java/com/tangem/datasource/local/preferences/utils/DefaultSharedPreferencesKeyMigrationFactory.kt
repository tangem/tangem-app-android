package com.tangem.datasource.local.preferences.utils

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.datasource.local.preferences.migration.SharedPreferencesKeyMigrationFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class DefaultSharedPreferencesKeyMigrationFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : SharedPreferencesKeyMigrationFactory {

    override fun create(legacyPrefsName: String, legacyKeyName: String, keyName: String): DataMigration<Preferences> {
        return SharedPreferencesKeyMigration(
            context = context,
            legacyPrefsName = legacyPrefsName,
            legacyKeyName = legacyKeyName,
            keyName = keyName,
            analyticsExceptionHandler = analyticsExceptionHandler,
        )
    }

    override fun createBulk(legacyPrefsName: String, keyNames: Set<String>): DataMigration<Preferences> {
        return SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = legacyPrefsName,
            keysToMigrate = keyNames,
        )
    }
}