package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.SocketChannelFactory
import com.tangem.utils.coroutines.AppCoroutineScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketPriceSocketModule {

    @Provides
    @Singleton
    fun providePolymarketPriceSubscriptions(
        factory: SocketChannelFactory,
        appScope: AppCoroutineScope,
        credentialsSource: PolymarketPriceCredentialsSource,
    ): PolymarketPriceSubscriptions = PolymarketPriceSubscriptions.create(
        factory = factory,
        scope = appScope,
        credentials = credentialsSource.credentials,
    )
}