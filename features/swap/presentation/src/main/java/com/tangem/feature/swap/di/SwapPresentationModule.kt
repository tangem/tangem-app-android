package com.tangem.feature.swap.di

import com.tangem.domain.tokens.GetCryptoCurrencyStatusSyncUseCase
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.feature.swap.domain.*
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
class SwapPresentationModule {

    @ViewModelScoped
    @Provides
    fun providesGetCryptoCurrenciesUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatcherProvider: CoroutineDispatcherProvider,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): GetCryptoCurrencyStatusSyncUseCase {
        return GetCryptoCurrencyStatusSyncUseCase(
            currenciesRepository = currenciesRepository,
            quotesRepository = quotesRepository,
            networksRepository = networksRepository,
            dispatchers = dispatcherProvider,
        )
    }
}