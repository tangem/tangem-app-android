package com.tangem.data.polymarket

import com.tangem.data.polymarket.price.DefaultPolymarketPriceCredentialsSource
import com.tangem.data.polymarket.price.PolymarketCryptoPriceSeriesReducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketCredentialsStore
import com.tangem.domain.polymarket.PolymarketCryptoPriceRepository
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceFeed
import com.tangem.domain.polymarket.model.PolymarketCryptoPriceSeries
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceAuthDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceFeedKey
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceMessageDto
import com.tangem.grow.datasource.polymarket.socket.PolymarketPriceSubscriptions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class DefaultPolymarketCryptoPriceRepository @Inject constructor(
    private val subscriptions: PolymarketPriceSubscriptions,
    private val credentialsSource: DefaultPolymarketPriceCredentialsSource,
    private val credentialsStore: PolymarketCredentialsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketCryptoPriceRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observePrices(
        userWalletId: UserWalletId,
        feed: PolymarketCryptoPriceFeed,
    ): Flow<PolymarketCryptoPriceSeries> {
        val key = PolymarketPriceFeedKey(
            channel = feed.channel,
            symbol = feed.symbol,
            provider = feed.provider,
            windowSeconds = feed.windowSeconds,
        )

        return credentialsStore.hasCredentials(userWalletId)
            .map { isStored -> if (isStored) readCredentials(userWalletId) else null }
            .distinctUntilChanged()
            .transformLatest { credentials ->
                if (credentials == null) {
                    emit(PolymarketCryptoPriceSeries(status = PolymarketCryptoPriceSeries.Status.UNAVAILABLE))
                } else {
                    credentialsSource.authorize(credentials) { emitAll(observeSeries(key)) }
                }
            }
            .flowOn(dispatchers.default)
    }

    private fun observeSeries(key: PolymarketPriceFeedKey): Flow<PolymarketCryptoPriceSeries> = channelFlow {
        val series = MutableStateFlow(PolymarketCryptoPriceSeries())
        launch {
            subscriptions.observeState(key).collect { state ->
                series.update { PolymarketCryptoPriceSeriesReducer.reduce(it, state) }
            }
        }
        launch {
            var isSnapshotRequested = false
            subscriptions.observe(key).collect { message ->
                // A stream somebody else already observes sends a newcomer ticks only
                if (!isSnapshotRequested && message.isTick() && series.value.points.isEmpty()) {
                    isSnapshotRequested = true
                    subscriptions.resubscribe(key)
                }
                series.update { PolymarketCryptoPriceSeriesReducer.reduce(it, message) }
            }
        }
        series.collect { send(it) }
    }

    private suspend fun readCredentials(userWalletId: UserWalletId): PolymarketPriceAuthDto.Credentials? {
        val stored = runSuspendCatching { credentialsStore.get(userWalletId) }
            .onFailure { TangemLogger.e("Failed to read the API credentials of $userWalletId", it) }
            .getOrNull()
            ?: return null

        return PolymarketPriceAuthDto.Credentials(
            apiKey = stored.apiKey,
            secret = stored.secret,
            passphrase = stored.passphrase,
        )
    }

    private fun PolymarketPriceMessageDto.isTick(): Boolean = payload != null && payload?.points == null
}