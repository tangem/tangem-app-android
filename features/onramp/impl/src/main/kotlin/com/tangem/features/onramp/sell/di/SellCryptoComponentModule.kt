package com.tangem.features.onramp.sell.di

import com.tangem.features.onramp.component.SellCryptoComponent
import com.tangem.features.onramp.sell.DefaultSellCryptoComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SellCryptoComponentModule {

    @Binds
    @Singleton
    fun bindSellCryptoComponentFactory(factory: DefaultSellCryptoComponent.Factory): SellCryptoComponent.Factory
}