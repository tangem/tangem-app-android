package com.tangem.features.polymarket.impl.main.model

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Lets the main screen's pull-to-refresh wait for the blocks it hosts.
 *
 * A block's model registers its refresh here rather than the host model holding the block: components are

 */
internal class PolymarketMainBlocksRefresh {

    private val refreshes = CopyOnWriteArrayList<suspend () -> Unit>()

    fun register(refresh: suspend () -> Unit) {
        refreshes += refresh
    }

    suspend fun refreshAll() {
        coroutineScope { refreshes.map { refresh -> async { refresh() } }.awaitAll() }
    }
}