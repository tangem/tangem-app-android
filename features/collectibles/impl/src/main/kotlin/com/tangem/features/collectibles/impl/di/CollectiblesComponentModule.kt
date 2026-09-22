package com.tangem.features.collectibles.impl.di

import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockComponent
import com.tangem.features.collectibles.impl.DefaultCollectiblesEntryComponent
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.DefaultCollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.walletblock.DefaultCollectiblesWalletBlockComponent
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface CollectiblesComponentModule {

    @Binds
    @Singleton
    fun bindCollectiblesEntryComponentFactory(
        impl: DefaultCollectiblesEntryComponent.Factory,
    ): CollectiblesEntryComponent.Factory

    @Binds
    @Singleton
    fun bindCollectiblesDiscoveryComponentFactory(
        impl: DefaultCollectiblesDiscoveryComponent.Factory,
    ): CollectiblesDiscoveryComponent.Factory

    @Binds
    @Singleton
    fun bindCollectiblesWalletBlockComponentFactory(
        impl: DefaultCollectiblesWalletBlockComponent.Factory,
    ): CollectiblesWalletBlockComponent.Factory
}