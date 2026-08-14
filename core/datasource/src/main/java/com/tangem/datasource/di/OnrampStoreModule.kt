package com.tangem.datasource.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.core.local.datastore.RuntimeSharedStore
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
import com.tangem.datasource.local.onramp.country.DefaultOnrampCurrentCountryByIPStore
import com.tangem.datasource.local.onramp.country.OnrampCurrentCountryByIPStore
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
        return DefaultOnrampPaymentMethodsStore(store = RuntimeSharedMapStore())
    }

    @Provides
    @Singleton
    fun provideOnrampPairsStore(): OnrampPairsStore {
        return DefaultOnrampPairsStore(store = RuntimeSharedMapStore())
    }

    @Provides
    @Singleton
    fun provideOnrampQuotesStore(): OnrampQuotesStore {
        return DefaultOnrampQuotesStore(store = RuntimeSharedMapStore())
    }

    @Provides
    @Singleton
    fun provideOnrampCountriesStore(): OnrampCountriesStore {
        return DefaultOnrampCountriesStore(store = RuntimeSharedMapStore())
    }

    @Provides
    @Singleton
    fun provideOnrampCurrencies(): OnrampCurrenciesStore {
        return DefaultOnrampCurrenciesStore(store = RuntimeSharedMapStore())
    }

    @Provides
    @Singleton
    fun provideOnrampCurrentCountryByIPStore(): OnrampCurrentCountryByIPStore {
        return DefaultOnrampCurrentCountryByIPStore(store = RuntimeSharedStore())
    }
}