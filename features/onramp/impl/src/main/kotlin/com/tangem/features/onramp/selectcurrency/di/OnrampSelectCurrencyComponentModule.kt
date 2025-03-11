package com.tangem.features.onramp.selectcurrency.di

import com.tangem.features.onramp.selectcurrency.DefaultSelectCurrencyComponent
import com.tangem.features.onramp.selectcurrency.SelectCurrencyComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface OnrampSelectCurrencyComponentModule {

    @Binds
    @Singleton
    fun bindSelectCurrencyComponentFactory(
        factory: DefaultSelectCurrencyComponent.Factory,
    ): SelectCurrencyComponent.Factory
}