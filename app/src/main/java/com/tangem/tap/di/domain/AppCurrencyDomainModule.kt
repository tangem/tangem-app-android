package com.tangem.tap.di.domain

import com.tangem.domain.appcurrency.FetchAppCurrenciesUseCase
import com.tangem.domain.appcurrency.GetAvailableCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.SelectAppCurrencyUseCase
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object AppCurrencyDomainModule {

    @Provides
    @Singleton
    fun provideGetSelectedAppCurrencyUseCase(
        appCurrencyRepository: AppCurrencyRepository,
    ): GetSelectedAppCurrencyUseCase {
        return GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    @Provides
    @Singleton
    fun provideSelectAppCurrencyUseCase(appCurrencyRepository: AppCurrencyRepository): SelectAppCurrencyUseCase {
        return SelectAppCurrencyUseCase(appCurrencyRepository)
    }

    @Provides
    @Singleton
    fun provideGetAvailableCurrenciesUseCase(
        appCurrencyRepository: AppCurrencyRepository,
    ): GetAvailableCurrenciesUseCase {
        return GetAvailableCurrenciesUseCase(appCurrencyRepository)
    }

    @Provides
    @Singleton
    fun provideFetchAppCurrenciesUseCase(appCurrencyRepository: AppCurrencyRepository): FetchAppCurrenciesUseCase {
        return FetchAppCurrenciesUseCase(appCurrencyRepository)
    }
}