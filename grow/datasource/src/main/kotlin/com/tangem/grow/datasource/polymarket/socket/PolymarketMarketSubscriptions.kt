package com.tangem.grow.datasource.polymarket.socket

import com.tangem.core.socket.KeyedSubscriptions
import com.tangem.core.socket.SocketState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class PolymarketMarketSubscriptions internal constructor(
    private val subscriptions: KeyedSubscriptions<String, PolymarketMarketSocket>,
) {

    fun observeState(assetIds: Set<String>): Flow<SocketState> {
        val keys = assetIds.filterTo(mutableSetOf(), String::isNotBlank)
        if (keys.isEmpty()) return emptyFlow()
        return subscriptions.observe(keys, PolymarketMarketSocket::state) { keys }.distinctUntilChanged()
    }

    fun resubscribe(assetIds: Set<String>) = subscriptions.resubscribe(assetIds)

    fun observe(assetIds: Set<String>): Flow<PolymarketMarketEventDto> {
        val keys = assetIds.filterTo(mutableSetOf(), String::isNotBlank)
        if (keys.isEmpty()) return emptyFlow()

        return subscriptions.observe(keys, PolymarketMarketSocket::events, ::assetIdsOf)
            .map { event ->
                if (event is PolymarketMarketEventDto.PriceChange) {
                    event.copy(priceChanges = event.priceChanges.filter { it.assetId in keys })
                } else {
                    event
                }
            }
    }

    private fun assetIdsOf(event: PolymarketMarketEventDto): List<String> = when (event) {
        is PolymarketMarketEventDto.Book -> listOf(event.assetId)
        is PolymarketMarketEventDto.PriceChange -> event.priceChanges.map { it.assetId }
        is PolymarketMarketEventDto.BestBidAsk -> listOf(event.assetId)
        is PolymarketMarketEventDto.LastTradePrice -> listOf(event.assetId)
        is PolymarketMarketEventDto.Unknown -> emptyList()
    }
}