package com.tangem.data.polymarket

import com.tangem.core.socket.SocketState
import com.tangem.data.polymarket.quote.PolymarketQuoteConverter
import com.tangem.data.polymarket.quote.PolymarketQuoteStore
import com.tangem.domain.polymarket.PolymarketLiveQuotesRepository
import com.tangem.domain.polymarket.model.PolymarketLiveQuote
import com.tangem.grow.datasource.polymarket.socket.PolymarketMarketSubscriptions
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

internal class DefaultPolymarketLiveQuotesRepository @Inject constructor(
    private val subscriptions: PolymarketMarketSubscriptions,
    private val store: PolymarketQuoteStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : PolymarketLiveQuotesRepository {

    override fun observeLiveQuotes(assetIds: Set<String>): Flow<Map<String, PolymarketLiveQuote>> {
        val keys = assetIds.filterTo(mutableSetOf(), String::isNotBlank)
        if (keys.isEmpty()) return flowOf(emptyMap())
        return channelFlow {
            val connection = MutableStateFlow<SocketState>(SocketState.Idle)
            launch {
                subscriptions.observeState(keys).collect { connection.value = it }
            }
            launch {
                subscriptions.observe(keys).collect { store.apply(PolymarketQuoteConverter.convert(it)) }
            }
            while (isActive) {
                val snapshot = store.snapshot(keys, isConnected = connection.value == SocketState.Connected)
                if (snapshot.resubscribe.isNotEmpty()) subscriptions.resubscribe(snapshot.resubscribe)
                send(snapshot.quotes)
                delay(UPDATE_INTERVAL)
            }
        }.conflate().flowOn(dispatchers.default).distinctUntilChanged()
    }

    private companion object {
        val UPDATE_INTERVAL = 100.milliseconds
    }
}