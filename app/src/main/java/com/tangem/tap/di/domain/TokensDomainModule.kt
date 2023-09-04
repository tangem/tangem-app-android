package com.tangem.tap.di.domain

import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.MarketCryptoCurrencyRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.tokens.repository.QuotesRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
internal object TokensDomainModule {

    @Provides
    @ViewModelScoped
    fun provideFetchTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchTokenListUseCase {
        return FetchTokenListUseCase(currenciesRepository, networksRepository, quotesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideGetTokenListUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetTokenListUseCase {
        return GetTokenListUseCase(currenciesRepository, quotesRepository, networksRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideRemoveCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): RemoveCurrencyUseCase {
        return RemoveCurrencyUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCurrencyStatusUpdatesUseCase {
        return GetCurrencyStatusUpdatesUseCase(currenciesRepository, quotesRepository, networksRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideGetPrimaryCurrencyUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetPrimaryCurrencyStatusUpdatesUseCase {
        return GetPrimaryCurrencyStatusUpdatesUseCase(
            currenciesRepository,
            quotesRepository,
            networksRepository,
            dispatchers,
        )
    }

    @Provides
    @ViewModelScoped
    fun provideFetchCurrencyStatusUseCase(
        currenciesRepository: CurrenciesRepository,
        quotesRepository: QuotesRepository,
        networksRepository: NetworksRepository,
    ): FetchCurrencyStatusUseCase {
        return FetchCurrencyStatusUseCase(currenciesRepository, networksRepository, quotesRepository)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListGroupingUseCase(
        dispatchers: CoroutineDispatcherProvider,
    ): ToggleTokenListGroupingUseCase {
        return ToggleTokenListGroupingUseCase(dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideToggleTokenListSortingUseCase(dispatchers: CoroutineDispatcherProvider): ToggleTokenListSortingUseCase {
        return ToggleTokenListSortingUseCase(dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideApplyTokenListSortingUseCase(
        currenciesRepository: CurrenciesRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): ApplyTokenListSortingUseCase {
        return ApplyTokenListSortingUseCase(currenciesRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCryptoCurrencyActionsUseCase(
        rampStateManager: RampStateManager,
        marketCryptoCurrencyRepository: MarketCryptoCurrencyRepository,
        dispatchers: CoroutineDispatcherProvider,
    ): GetCryptoCurrencyActionsUseCase {
        return GetCryptoCurrencyActionsUseCase(rampStateManager, marketCryptoCurrencyRepository, dispatchers)
    }

    @Provides
    @ViewModelScoped
    fun provideGetCurrenciesUseCase(currenciesRepository: CurrenciesRepository): GetCryptoCurrenciesUseCase {
        return GetCryptoCurrenciesUseCase(currenciesRepository = currenciesRepository)
    }
}