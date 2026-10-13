package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.KeepAlive
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.SocketSpec
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketSportsSocketModule {

    @Provides
    @Singleton
    fun providePolymarketSportsSocket(factory: SocketChannelFactory): PolymarketSportsSocket {
        return factory.createPolymarketSportsSocket(
            SocketSpec(
                url = "wss://sports-api.polymarket.com/ws",
                keepAlive = KeepAlive.ServerSends(text = "ping", reply = "pong"),
            ),
        )
    }
}