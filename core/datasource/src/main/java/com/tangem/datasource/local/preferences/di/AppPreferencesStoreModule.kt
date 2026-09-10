package com.tangem.datasource.local.preferences.di

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.squareup.moshi.Moshi
import com.tangem.core.local.moshi.SdkMoshi
import com.tangem.datasource.local.preferences.*
import com.tangem.datasource.local.preferences.migration.CleanupKeyMigration
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppPreferencesStoreModule {

    private const val PREFERENCES_FILE_NAME = "TAP_PREFS"

    @Provides
    @Singleton
    fun provideAppPreferencesStore(
        @ApplicationContext context: Context,
        appScope: AppCoroutineScope,
        externalMigrations: Set<@JvmSuppressWildcards DataMigration<Preferences>>,
        dispatchers: CoroutineDispatcherProvider,
        @SdkMoshi moshi: Moshi,
    ): AppPreferencesStore {
        return DefaultAppPreferencesStore(
            preferencesDataStore = createPreferencesDataStore(
                context = context,
                appScope = appScope,
                externalMigrations = externalMigrations,
            ),
            moshi = moshi,
            dispatchers = dispatchers,
        )
    }

    private fun createPreferencesDataStore(
        context: Context,
        appScope: AppCoroutineScope,
        externalMigrations: Set<@JvmSuppressWildcards DataMigration<Preferences>>,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { corruptionException ->
                    TangemLogger.e("Error", corruptionException)
                    emptyPreferences()
                },
            ),
            migrations = externalMigrations.toList() + ownMigrations(),
            scope = appScope,
            produceFile = { context.preferencesDataStoreFile(name = PREFERENCES_FILE_NAME) },
        )
    }

    private fun ownMigrations(): List<DataMigration<Preferences>> {
        return listOf(
            CleanupKeyMigration(key = PreferencesKeys.APP_LOGS_KEY),
            CleanupKeyMigration(key = stringSetPreferencesKey(name = "tangemPayEligibility")),
            CleanupKeyMigration(key = stringSetPreferencesKey(name = "tangemPayEligibilityList")),
            CleanupKeyMigration(key = stringSetPreferencesKey(name = "tangemPayEligibilityListV2")),
        )
    }
}