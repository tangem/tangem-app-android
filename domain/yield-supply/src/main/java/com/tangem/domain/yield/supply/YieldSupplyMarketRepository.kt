package com.tangem.domain.yield.supply

import com.tangem.domain.yield.supply.models.YieldMarketToken
import kotlinx.coroutines.flow.Flow

interface YieldSupplyMarketRepository {

    /**
     * Get cached yield markets or null if nothing cached yet.
     */
    suspend fun getCachedMarkets(): List<YieldMarketToken>?

    /**
     * Update markets by fetching from network and cache the result. Returns latest markets.
     */
    @Throws
    suspend fun updateMarkets(): List<YieldMarketToken>

    /**
     * Observe runtime markets updates.
     */
    fun getMarketsFlow(): Flow<List<YieldMarketToken>>
}