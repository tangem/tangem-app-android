package com.tangem.grow.datasource.yield.local

import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import kotlinx.coroutines.flow.Flow

interface YieldMarketsStore {

    fun get(): Flow<List<YieldSupplyMarketTokenDto>>

    suspend fun getSyncOrNull(): List<YieldSupplyMarketTokenDto>?

    suspend fun store(items: List<YieldSupplyMarketTokenDto>)
}