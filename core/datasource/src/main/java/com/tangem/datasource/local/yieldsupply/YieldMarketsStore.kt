package com.tangem.datasource.local.yieldsupply

import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import kotlinx.coroutines.flow.Flow

interface YieldMarketsStore {

    fun get(): Flow<List<YieldSupplyMarketTokenDto>>

    suspend fun getSyncOrNull(): List<YieldSupplyMarketTokenDto>?

    suspend fun store(items: List<YieldSupplyMarketTokenDto>)
}