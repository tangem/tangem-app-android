package com.tangem.data.swap.di

import com.squareup.moshi.Moshi
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.express.converter.ExpressErrorConverter
import com.tangem.data.swap.DefaultSwapErrorResolver
import com.tangem.data.swap.DefaultSwapRepositoryV2
import com.tangem.data.swap.DefaultSwapTransactionRepository
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.express.ExpressRepository
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.SwapTransactionRepository
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
        dataSignatureVerifier: DataSignatureVerifier,
        singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
        singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
        @NetworkMoshi moshi: Moshi,
    ): SwapRepositoryV2 {
        return DefaultSwapRepositoryV2(
            tangemExpressApi = tangemExpressApi,
            expressRepository = expressRepository,
            coroutineDispatcher = coroutineDispatcher,
            appPreferencesStore = appPreferencesStore,
            dataSignatureVerifier = dataSignatureVerifier,
            moshi = moshi,
            singleQuoteStatusSupplier = singleQuoteStatusSupplier,
            singleQuoteStatusFetcher = singleQuoteStatusFetcher,
        )
    }

    @Provides
    @Singleton
    fun provideSwapTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
        networkFactory: NetworkFactory,
        multiAccountListSupplier: MultiAccountListSupplier,
        accountsFeatureToggles: AccountsFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ): SwapTransactionRepository {
        return DefaultSwapTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            responseCryptoCurrenciesFactory = responseCryptoCurrenciesFactory,
            networkFactory = networkFactory,
            multiAccountListSupplier = multiAccountListSupplier,
            accountsFeatureToggles = accountsFeatureToggles,
            dispatchers = dispatchers,
        )
    }
}