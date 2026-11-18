package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.ClosePolicy
import com.tangem.core.socket.KeyedSubscriptions
import com.tangem.core.socket.SocketChannelFactory
import com.tangem.core.socket.SocketClose
import com.tangem.core.socket.SocketLifecycle
import com.tangem.core.socket.SocketSpec
import com.tangem.core.socket.SocketState
import com.tangem.grow.datasource.BuildConfig
import com.tangem.grow.datasource.config.PolymarketClob
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto.Companion.OP_ERROR
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceSubscriptionsDto.Operation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class PolymarketPriceSubscriptions internal constructor(
    private val subscriptions: KeyedSubscriptions<PolymarketPriceFeedKey, PolymarketPriceSocket>,
) {

    /**
     * Price frames of the [key]'s stream, and the errors the server answers on its channel or on the connection, so
     * that the consumer can tell a refused subscription from a quiet one. An error names its channel only: with two
     * feeds of one channel observed at once, each sees the other's error.
     */
    fun observe(key: PolymarketPriceFeedKey): Flow<PolymarketPriceMessageDto> {
        val keys = setOf(key)

        return subscriptions.observe(keys, PolymarketPriceSocket::messages) { keys }.filter { message ->
            if (message.op == OP_ERROR) {
                message.channel == null || message.channel == key.channel
            } else {
                message.belongsTo(key)
            }
        }
    }

    /** Connection state behind the [key]'s stream. Observing it does not open the socket: only [observe] does. */
    fun observeState(key: PolymarketPriceFeedKey): Flow<SocketState> {
        val keys = setOf(key)

        return subscriptions.observe(keys, PolymarketPriceSocket::state) { keys }.distinctUntilChanged()
    }

    internal companion object {

        /** The server serves at most this many subscriptions over one connection. */
        private const val MAX_SUBSCRIPTIONS_PER_CONNECTION = 64

        /**
         * @param credentials CLOB API credentials of the wallet in use. The socket stays closed while there are none
         * and reconnects when they change
         */
        fun create(
            factory: SocketChannelFactory,
            scope: CoroutineScope,
            credentials: StateFlow<PolymarketPriceAuthDto.Credentials?>,
        ): PolymarketPriceSubscriptions = PolymarketPriceSubscriptions(
            KeyedSubscriptions(
                scope = scope,
                newService = { _, openKeys ->
                    if (BuildConfig.MOCK_DATA_SOURCE) {
                        FakePolymarketPriceSocket()
                    } else {
                        factory.createPolymarketPriceSocket(
                            SocketSpec(
                                url = PolymarketClob.LIVE_DATA_SOCKET_URL,
                                lifecycle = SocketLifecycle { credentials.map { it != null } },
                                reconnectOn = credentials,
                                closePolicy = PolymarketPriceClosePolicy(),
                                onOpen = {
                                    val authorization = checkNotNull(credentials.value) {
                                        "No CLOB credentials to authorize the live-data socket with"
                                    }
                                    send(PolymarketPriceAuthDto.serializer(), PolymarketPriceAuthDto(authorization))
                                    val keys = openKeys()
                                    if (keys.isNotEmpty()) {
                                        send(
                                            PolymarketPriceSubscriptionsDto.serializer(),
                                            frame(Operation.SUBSCRIBE, keys),
                                        )
                                    }
                                },
                            ),
                        )
                    }
                },
                subscribe = { keys -> updateSubscriptions(frame(Operation.SUBSCRIBE, keys)) },
                unsubscribe = { keys -> updateSubscriptions(frame(Operation.UNSUBSCRIBE, keys)) },
                maxKeysPerService = MAX_SUBSCRIPTIONS_PER_CONNECTION,
            ),
        )

        private fun frame(operation: Operation, keys: Set<PolymarketPriceFeedKey>) = PolymarketPriceSubscriptionsDto(
            operation = operation,
            subscriptions = keys
                .sortedWith(compareBy(PolymarketPriceFeedKey::channel, PolymarketPriceFeedKey::symbol))
                .map { key ->
                    PolymarketPriceSubscriptionsDto.Subscription(
                        channel = key.channel,
                        filter = PolymarketPriceSubscriptionsDto.Filter(
                            symbol = key.symbol,
                            provider = key.provider,
                            windowSeconds = key.windowSeconds,
                        ),
                    )
                },
        )
    }
}

/**
 * A stream is told apart by its channel and symbol; the averaging window and the provider count only when both the
 * key and the frame name them. The server may spell a symbol either `btcusd` or `BTC/USD`.
 */
private fun PolymarketPriceMessageDto.belongsTo(key: PolymarketPriceFeedKey): Boolean {
    val payload = payload ?: return false

    return channel == key.channel &&
        payload.symbol?.normalizedSymbol() == key.symbol.normalizedSymbol() &&
        (key.windowSeconds == null || payload.windowSeconds == null || payload.windowSeconds == key.windowSeconds) &&
        (key.provider == null || payload.source == null || payload.source.equals(key.provider, ignoreCase = true))
}

private fun String.normalizedSymbol(): String = filter(Char::isLetterOrDigit).lowercase()

/**
 * Close codes of the live-data socket: `4001` rejects the credentials and `4008` reports a policy violation, so
 * neither is retried; `4003` announces a restart and asks for one reconnect at a random moment within ten seconds.
 */
internal class PolymarketPriceClosePolicy(private val random: Random = Random.Default) : ClosePolicy {

    override fun decide(close: SocketClose): ClosePolicy.Decision = when (close.code) {
        AUTHENTICATION_FAILED, POLICY_VIOLATION -> ClosePolicy.Decision.Stop
        SERVER_DRAINING -> ClosePolicy.Decision.RetryAfter(random.nextLong(MAX_DRAINING_DELAY_MILLIS + 1).milliseconds)
        else -> ClosePolicy.Decision.Retry
    }

    private companion object {
        const val AUTHENTICATION_FAILED = 4001
        const val SERVER_DRAINING = 4003
        const val POLICY_VIOLATION = 4008
        const val MAX_DRAINING_DELAY_MILLIS = 10_000L
    }
}