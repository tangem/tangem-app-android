package com.tangem.features.collectibles.impl.di

import com.tangem.features.collectibles.api.CollectiblesEntryComponent
import com.tangem.features.collectibles.api.walletblock.CollectiblesWalletBlockComponent
import com.tangem.features.collectibles.impl.DefaultCollectiblesEntryComponent
import com.tangem.features.collectibles.impl.discovery.CollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.discovery.DefaultCollectiblesDiscoveryComponent
import com.tangem.features.collectibles.impl.packopening.video.DefaultPackRevealVideoPlayerFactory
import com.tangem.features.collectibles.impl.packopening.video.PackRevealVideoPlayer
import com.tangem.features.collectibles.impl.packopening.video.source.DefaultPackRevealMediaSourceFactory
import com.tangem.features.collectibles.impl.packopening.video.source.PackRevealMediaSourceFactory
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

    @Binds
    @Singleton
    fun bindPackRevealVideoPlayerFactory(impl: DefaultPackRevealVideoPlayerFactory): PackRevealVideoPlayer.Factory

    @Binds
    @Singleton
    fun bindPackRevealMediaSourceFactory(impl: DefaultPackRevealMediaSourceFactory): PackRevealMediaSourceFactory
}