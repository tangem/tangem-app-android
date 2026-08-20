package com.tangem.datasource.local.preferences.utils

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.*
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.utils.logging.TangemLogger
import java.io.File

/**
 * Migration of a specified key with name changing.
 * Example, migrate the "key1" from "pref1" to the "key2" from "pref2".
 *
 * @property context                   context
 * @property legacyPrefsName           legacy SharedPreferences name
 * @property legacyKeyName             legacy SharedPreferences key name
 * @property keyName                   new SharedPreferences key name
 * @property analyticsExceptionHandler reports a failed cleanup as a non-fatal event
 */
internal class SharedPreferencesKeyMigration(
    private val context: Context,
    private val legacyPrefsName: String,
    private val legacyKeyName: String,
    private val keyName: String,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : DataMigration<Preferences> {

    private val legacyPrefs by lazy {
        context.getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
    }

    override suspend fun cleanUp() {
        val sharedPrefsEditor = legacyPrefs.edit()

        sharedPrefsEditor.remove(legacyKeyName)

        if (!sharedPrefsEditor.commit()) {
            reportFailedCleanUp()
            return
        }

        if (legacyPrefs.all.isEmpty()) {
            deleteSharedPreferences(context = context, name = legacyPrefsName)
        }
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val currentKeys = currentData.asMap().keys.map(Preferences.Key<*>::name)

        // If migration is already happened, return
        if (currentKeys.contains(keyName)) return currentData

        val value = legacyPrefs.all[legacyKeyName]
        if (value != null) {
            val mutablePreferences = currentData.toMutablePreferences()

            when (value) {
                is Boolean -> mutablePreferences[booleanPreferencesKey(keyName)] = value
                is Float -> mutablePreferences[floatPreferencesKey(keyName)] = value
                is Int -> mutablePreferences[intPreferencesKey(keyName)] = value
                is Long -> mutablePreferences[longPreferencesKey(keyName)] = value
                is String -> mutablePreferences[stringPreferencesKey(keyName)] = value
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    mutablePreferences[stringSetPreferencesKey(keyName)] = value as Set<String>
                }
            }

            return mutablePreferences.toPreferences()
        }

        return currentData
    }

    /**
     * Deleting the already-migrated legacy key is best-effort: [android.content.SharedPreferences.Editor.commit]
     * returns false when the change cannot be persisted (e.g. the device is out of storage). That leaves a redundant
     * legacy key behind but does not affect the completed migration, so the failure is reported instead of being
     * rethrown — a throw here propagates through the DataStore initialization and crashes the app on startup.
     */
    private fun reportFailedCleanUp() {
        val exception = LegacyPreferencesCleanUpException(prefsName = legacyPrefsName)
        val params = mapOf(
            "prefs" to legacyPrefsName,
            "key" to legacyKeyName,
            "keys_left" to legacyPrefs.all.size.toString(),
            "usable_space" to File(context.applicationInfo.dataDir).usableSpace.toString(),
        )

        TangemLogger.e(messageString = "${exception.message}. Diagnostics: $params", throwable = exception)
        analyticsExceptionHandler.sendException(ExceptionAnalyticsEvent(exception = exception, params = params))
    }

    private fun deleteSharedPreferences(context: Context, name: String) {
        val prefsFile = getSharedPrefsFile(context, name)
        val prefsBackup = getSharedPrefsBackup(prefsFile)

        prefsFile.delete()
        prefsBackup.delete()
    }

    private fun getSharedPrefsFile(context: Context, name: String): File {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(prefsDir, "$name.xml")
    }

    private fun getSharedPrefsBackup(prefsFile: File) = File(prefsFile.path + ".bak")
}

/** Non-fatal marker reported when already-migrated legacy SharedPreferences keys cannot be deleted. */
internal class LegacyPreferencesCleanUpException(prefsName: String) :
    Exception("Unable to delete migrated keys from SharedPreferences: $prefsName")