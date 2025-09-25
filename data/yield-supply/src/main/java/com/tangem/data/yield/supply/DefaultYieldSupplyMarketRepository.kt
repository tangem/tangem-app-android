package com.tangem.data.yield.supply

import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class DefaultYieldSupplyMarketRepository(
    private val techApi: TangemTechApi,
    private val store: YieldMarketsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyMarketRepository {

    override suspend fun getCachedMarkets(): List<YieldMarketToken>? = withContext(dispatchers.io) {
        store.getSyncOrNull()
    }

    override suspend fun updateMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val response = techApi.getYieldMarkets().getOrThrow()
        val domain = response.marketDtos.map(YieldMarketTokenConverter::convert)
        store.store(domain)
        domain
    }

    override fun getMarketsFlow(): Flow<List<YieldMarketToken>> = store.get()
}