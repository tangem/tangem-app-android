package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.onramp.countries.DefaultOnrampCountriesStore
import com.tangem.datasource.local.onramp.countries.OnrampCountriesStore
import com.tangem.datasource.local.onramp.currencies.DefaultOnrampCurrenciesStore
import com.tangem.datasource.local.onramp.currencies.OnrampCurrenciesStore
import com.tangem.datasource.local.onramp.pairs.DefaultOnrampPairsStore
import com.tangem.datasource.local.onramp.pairs.OnrampPairsStore
import com.tangem.datasource.local.onramp.paymentmethods.DefaultOnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.quotes.DefaultOnrampQuotesStore
import com.tangem.datasource.local.onramp.quotes.OnrampQuotesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampStoreModule {

    @Provides
    @Singleton
    fun provideOnrampPaymentMethodsStore(): OnrampPaymentMethodsStore {
        return DefaultOnrampPaymentMethodsStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideOnrampPairsStore(): OnrampPairsStore {
        return DefaultOnrampPairsStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideOnrampQuotesStore(): OnrampQuotesStore {
        return DefaultOnrampQuotesStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideOnrampCountriesStore(): OnrampCountriesStore {
        return DefaultOnrampCountriesStore(dataStore = RuntimeDataStore())
    }

    @Provides
    @Singleton
    fun provideOnrampCurrencies(): OnrampCurrenciesStore {
        return DefaultOnrampCurrenciesStore(dataStore = RuntimeDataStore())
    }
}