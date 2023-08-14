package com.tangem.data.appcurrency.di

import com.tangem.data.appcurrency.MockAppCurrencyRepository
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
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
    fun provideAppCurrencyRepository(): AppCurrencyRepository {
        return MockAppCurrencyRepository()
    }
}
