package com.tangem.data.nft.di

import com.tangem.core.remote.moshi.NetworkMoshiConfigurer
import com.tangem.data.nft.store.SolanaIdentifierJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal object NFTMoshiModule {

    @Provides
    @IntoSet
    fun provideNFTMoshiConfigurer(): NetworkMoshiConfigurer = NetworkMoshiConfigurer { builder ->
        builder.add(SolanaIdentifierJsonAdapterFactory)
    }
}