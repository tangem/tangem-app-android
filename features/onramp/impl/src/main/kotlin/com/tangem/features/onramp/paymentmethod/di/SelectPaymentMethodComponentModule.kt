package com.tangem.features.onramp.paymentmethod.di

import com.tangem.features.onramp.paymentmethod.DefaultSelectPaymentMethodComponent
import com.tangem.features.onramp.paymentmethod.SelectPaymentMethodComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SelectPaymentMethodComponentModule {

    @Binds
    @Singleton
    fun bindSelectPaymentMethodComponentFactory(
        factory: DefaultSelectPaymentMethodComponent.Factory,
    ): SelectPaymentMethodComponent.Factory
}