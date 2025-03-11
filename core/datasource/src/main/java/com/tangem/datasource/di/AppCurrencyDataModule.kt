package com.tangem.datasource.di

import com.tangem.datasource.local.appcurrency.AvailableAppCurrenciesStore
import com.tangem.datasource.local.appcurrency.implementation.DefaultAvailableAppCurrenciesStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
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
}