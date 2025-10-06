package com.tangem.datasource.local.yieldsupply

import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import kotlinx.coroutines.flow.Flow

interface YieldMarketsStore {

    fun get(): Flow<List<YieldMarketsResponse.MarketDto>>

    suspend fun getSyncOrNull(): List<YieldMarketsResponse.MarketDto>?

    suspend fun store(items: List<YieldMarketsResponse.MarketDto>)
}