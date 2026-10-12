package com.tangem.grow.datasource.di

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.grow.datasource.preferences.migration.SwapCurrencyIdMigration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object GrowPreferencesMigrationsModule {

    @Provides
    @IntoSet
    fun provideSwapCurrencyIdMigration(keys: GrowPreferencesKeys): DataMigration<Preferences> {
        return SwapCurrencyIdMigration(keys)
    }
}