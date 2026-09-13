package com.tangem.data.onramp.di

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.local.onramp.paymentmethods.DefaultOnrampPaymentMethodsStore
import com.tangem.datasource.local.onramp.paymentmethods.OnrampPaymentMethodsStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object OnrampPaymentMethodsStoreModule {

    @Provides
    @Singleton
    fun provideOnrampPaymentMethodsStore(): OnrampPaymentMethodsStore {
        return DefaultOnrampPaymentMethodsStore(store = RuntimeSharedMapStore())
    }
}