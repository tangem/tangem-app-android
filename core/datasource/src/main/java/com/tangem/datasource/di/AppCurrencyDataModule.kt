package com.tangem.datasource.di

import com.tangem.datasource.appcurrency.AppCurrencyResponseStore
import com.tangem.datasource.appcurrency.DefaultAppCurrencyResponseStore
import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.appcurrency.implementation.DefaultAvailableAppCurrenciesStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppCurrencyDataModule {

    @Provides
    @Singleton
    fun provideAvailableAppCurrenciesStore(): AvailableAppCurrenciesStore {
        return DefaultAvailableAppCurrenciesStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideAppCurrencyResponseStore(appPreferencesStore: AppPreferencesStore): AppCurrencyResponseStore {
        return DefaultAppCurrencyResponseStore(appPreferencesStore)
    }
}