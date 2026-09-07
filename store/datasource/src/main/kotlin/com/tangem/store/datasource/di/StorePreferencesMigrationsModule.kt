package com.tangem.store.datasource.di

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.tangem.datasource.appcurrency.DefaultAppCurrencyResponseStore
import com.tangem.datasource.local.preferences.migration.SharedPreferencesKeyMigrationFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object StorePreferencesMigrationsModule {

    @Provides
    @IntoSet
    fun provideSelectedAppCurrencyMigration(
        factory: SharedPreferencesKeyMigrationFactory,
    ): DataMigration<Preferences> {
        return factory.create(
            legacyPrefsName = "selected_app_currency",
            legacyKeyName = "key",
            keyName = DefaultAppCurrencyResponseStore.SELECTED_APP_CURRENCY_KEY.name,
        )
    }
}