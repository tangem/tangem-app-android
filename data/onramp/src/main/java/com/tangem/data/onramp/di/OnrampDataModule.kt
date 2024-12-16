package com.tangem.data.onramp.di

import com.squareup.moshi.Moshi
import com.tangem.data.onramp.DefaultOnrampErrorResolver
import com.tangem.data.onramp.DefaultOnrampRepository
import com.tangem.data.onramp.DefaultOnrampTransactionRepository
import com.tangem.data.onramp.converters.error.OnrampErrorConverter
import com.tangem.datasource.api.express.TangemExpressApi
import com.tangem.datasource.api.express.models.response.ExpressErrorResponse
import com.tangem.datasource.api.onramp.OnrampApi
import com.tangem.datasource.crypto.DataSignatureVerifier
import com.tangem.datasource.di.NetworkMoshi
import com.tangem.datasource.local.onramp.countries.OnrampCountriesStore
import com.tangem.datasource.local.onramp.currencies.OnrampCurrenciesStore
import com.tangem.datasource.local.onramp.pairs.OnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.OnrampQuotesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.domain.onramp.repositories.OnrampErrorResolver
import com.tangem.domain.onramp.repositories.OnrampRepository
import com.tangem.domain.onramp.repositories.OnrampTransactionRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampDataModule {

    @Provides
    @Singleton
    fun provideOnrampRepository(
        onrampApi: OnrampApi,
        expressApi: TangemExpressApi,
        dispatchers: CoroutineDispatcherProvider,
        appPreferencesStore: AppPreferencesStore,
        paymentMethodsStore: OnrampPaymentMethodsStore,
        pairsStore: OnrampPairsStore,
        quotesStore: OnrampQuotesStore,
        countriesStore: OnrampCountriesStore,
        currenciesStore: OnrampCurrenciesStore,
        walletManagersFacade: WalletManagersFacade,
        dataSignatureVerifier: DataSignatureVerifier,
        @NetworkMoshi moshi: Moshi,
    ): OnrampRepository {
        return DefaultOnrampRepository(
            onrampApi = onrampApi,
            expressApi = expressApi,
            dispatchers = dispatchers,
            appPreferencesStore = appPreferencesStore,
            paymentMethodsStore = paymentMethodsStore,
            pairsStore = pairsStore,
            quotesStore = quotesStore,
            currenciesStore = currenciesStore,
            countriesStore = countriesStore,
            walletManagersFacade = walletManagersFacade,
            dataSignatureVerifier = dataSignatureVerifier,
            moshi = moshi,
        )
    }

    @Provides
    @Singleton
    fun provideOnrampTransactionRepository(
        appPreferencesStore: AppPreferencesStore,
        dispatchers: CoroutineDispatcherProvider,
    ): OnrampTransactionRepository {
        return DefaultOnrampTransactionRepository(
            appPreferencesStore = appPreferencesStore,
            dispatchers = dispatchers,
        )
    }

    @Provides
    @Singleton
    fun provideOnrampErrorResolver(@NetworkMoshi moshi: Moshi): OnrampErrorResolver {
        val jsonAdapter = moshi.adapter(ExpressErrorResponse::class.java)
        return DefaultOnrampErrorResolver(
            OnrampErrorConverter(jsonAdapter),
        )
    }
}