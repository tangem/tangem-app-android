package com.tangem.data.swap.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.data.swap.DefaultSwapErrorResolver
import com.tangem.data.swap.DefaultSwapRepositoryV2
import com.tangem.data.swap.DefaultSwapTransactionRepository
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.SwapTransactionRepository
import com.tangem.domain.tokens.operations.BaseCurrencyStatusOperations
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
internal object SwapDataModule {

    @Provides
    @Singleton
    fun provideSwapErrorResolver(@NetworkMoshi moshi: Moshi): SwapErrorResolver {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return DefaultSwapErrorResolver(
            ExpressErrorConverter(jsonAdapter),
        )
    }

    @Provides
    @Singleton
    fun provideSwapRepository(
        tangemExpressApi: TangemExpressApi,
        expressRepository: ExpressRepository,
        coroutineDispatcher: CoroutineDispatcherProvider,
        appPreferencesStore: AppPreferencesStore,
        currencyStatusOperations: BaseCurrencyStatusOperations,
    ): SwapRepositoryV2 {
        return DefaultSwapRepositoryV2(
            tangemExpressApi = tangemExpressApi,
            expressRepository = expressRepository,
            coroutineDispatcher = coroutineDispatcher,
            appPreferencesStore = appPreferencesStore,
            currencyStatusOperations = currencyStatusOperations,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        dispatchers: CoroutineDispatcherProvider,
    ): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
            dispatchers = dispatchers,
        )
    }
}