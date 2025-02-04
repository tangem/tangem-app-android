package com.tangem.features.onramp.hottokens.di

import com.tangem.features.onramp.hottokens.DefaultHotCryptoComponent
import com.tangem.features.onramp.hottokens.HotCryptoComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface HotCryptoComponentModule {

    @Binds
    @Singleton
    fun bindHotCryptoComponentFactory(factory: DefaultHotCryptoComponent.Factory): HotCryptoComponent.Factory
}