package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.KeepAlive
import com.tangem.core.socket.KeyedSubscriptions
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.SocketSpec
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.config.PolymarketClob
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.serialization.SerializersModuleProvider
import com.tangem.utils.serialization.polymorphicUnknownFallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.modules.SerializersModule
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
internal object PolymarketMarketSocketModule {

    @Provides
    @Singleton
    fun providePolymarketMarketSubscriptions(
        factory: SocketChannelFactory,
        appScope: AppCoroutineScope,
    ): PolymarketMarketSubscriptions = PolymarketMarketSubscriptions(
        KeyedSubscriptions(
            scope = appScope,
            newService = { _, openKeys ->
                if (BuildConfig.MOCK_DATA_SOURCE) {
                    FakePolymarketMarketSocket()
                } else {
                    factory.createPolymarketMarketSocket(
                        SocketSpec(
                            url = PolymarketClob.MARKET_SOCKET_URL,
                            keepAlive = KeepAlive.ClientSends(text = "PING", interval = 10.seconds),
                            isSplitArrayFrames = true,
                            onOpen = {
                                send(
                                    PolymarketMarketSubscriptionDto.serializer(),
                                    PolymarketMarketSubscriptionDto(assetIds = openKeys().sorted()),
                                )
                            },
                        ),
                    )
                }
            },
            subscribe = { keys ->
                updateSubscription(
                    PolymarketMarketSubscriptionUpdateDto(
                        assetIds = keys.sorted(),
                        operation = PolymarketMarketSubscriptionUpdateDto.Operation.SUBSCRIBE,
                    ),
                )
            },
            unsubscribe = { keys ->
                updateSubscription(
                    PolymarketMarketSubscriptionUpdateDto(
                        assetIds = keys.sorted(),
                        operation = PolymarketMarketSubscriptionUpdateDto.Operation.UNSUBSCRIBE,
                    ),
                )
            },
        ),
    )

    @Provides
    @IntoSet
    fun providePolymarketMarketSerializersModule(): SerializersModuleProvider = SerializersModuleProvider {
        SerializersModule {
            polymorphicUnknownFallback<PolymarketMarketEventDto>(PolymarketMarketEventDto::Unknown)
        }
    }
}