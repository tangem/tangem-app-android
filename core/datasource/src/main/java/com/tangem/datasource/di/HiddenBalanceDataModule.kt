package com.tangem.datasource.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.tangem.datasource.local.appcurrency.BalanceHidingSettingsStore
import com.tangem.datasource.local.appcurrency.implementation.BalanceStateHidingSettingsStore
import com.tangem.datasource.local.datastore.JsonSharedPreferencesDataStore
import com.tangem.domain.balancehiding.BalanceHidingSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object HiddenBalanceDataModule {

    @Provides
    fun provideHiddenBalanceStateStore(
        @ApplicationContext context: Context,
        @NetworkMoshi moshi: Moshi,
    ): BalanceHidingSettingsStore {
        return BalanceStateHidingSettingsStore(
            dataStore = JsonSharedPreferencesDataStore(
                preferencesName = "balance_hiding_settings",
                context = context,
                adapter = moshi.adapter(BalanceHidingSettings::class.java),
            ),
        )
    }
}