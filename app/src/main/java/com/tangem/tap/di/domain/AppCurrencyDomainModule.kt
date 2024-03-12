package com.tangem.tap.di.domain

import com.tangem.domain.appcurrency.FetchAppCurrenciesUseCase
import com.tangem.domain.appcurrency.GetAvailableCurrenciesUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.SelectAppCurrencyUseCase
import com.tangem.domain.appcurrency.repository.AppCurrencyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object AppCurrencyDomainModule {

    @Provides
    fun provideGetSelectedAppCurrencyUseCase(
        appCurrencyRepository: AppCurrencyRepository,
    ): GetSelectedAppCurrencyUseCase {
        return GetSelectedAppCurrencyUseCase(appCurrencyRepository)
    }

    @Provides
    fun provideSelectAppCurrencyUseCase(appCurrencyRepository: AppCurrencyRepository): SelectAppCurrencyUseCase {
        return SelectAppCurrencyUseCase(appCurrencyRepository)
    }

    @Provides
    fun provideGetAvailableCurrenciesUseCase(
        appCurrencyRepository: AppCurrencyRepository,
    ): GetAvailableCurrenciesUseCase {
        return GetAvailableCurrenciesUseCase(appCurrencyRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideFetchAppCurrenciesUseCase(appCurrencyRepository: AppCurrencyRepository): FetchAppCurrenciesUseCase {
        return FetchAppCurrenciesUseCase(appCurrencyRepository)
    }
}