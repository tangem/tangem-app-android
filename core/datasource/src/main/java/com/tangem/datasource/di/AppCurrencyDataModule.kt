package com.tangem.datasource.di

import com.tangem.datasource.local.appcurrency.MockSelectedAppCurrencyStore
import com.tangem.datasource.local.appcurrency.SelectedAppCurrencyStore
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
    fun provideSelectedAppCurrencyStore(): SelectedAppCurrencyStore {
        return MockSelectedAppCurrencyStore()
    }
}