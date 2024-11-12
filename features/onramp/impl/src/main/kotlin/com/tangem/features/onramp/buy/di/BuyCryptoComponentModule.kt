package com.tangem.features.onramp.buy.di

import com.tangem.features.onramp.buy.DefaultBuyCryptoComponent
import com.tangem.features.onramp.component.BuyCryptoComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface BuyCryptoComponentModule {

    @Binds
    @Singleton
    fun bindBuyCryptoComponentFactory(factory: DefaultBuyCryptoComponent.Factory): BuyCryptoComponent.Factory
}