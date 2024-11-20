package com.tangem.datasource.di

import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.onramp.DefaultOnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.OnrampPaymentMethodsStore
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
}
