package com.tangem.datasource.local.yieldsupply

import com.tangem.domain.yield.supply.models.YieldMarketToken
import kotlinx.coroutines.flow.Flow

interface YieldMarketsStore {

    fun get(): Flow<List<YieldMarketToken>>

    suspend fun getSyncOrNull(): List<YieldMarketToken>?

    suspend fun store(items: List<YieldMarketToken>)
}