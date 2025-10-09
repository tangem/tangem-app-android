package com.tangem.data.yield.supply

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.data.yield.supply.converters.YieldTokenStatusConverter
import com.tangem.data.yield.supply.converters.YieldTokenChartConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.yield.supply.YieldSupplyMarketRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldMarketTokenStatus
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.collections.map

internal class DefaultYieldSupplyMarketRepository(
    private val yieldSupplyApi: YieldSupplyApi,
    private val store: YieldMarketsStore,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyMarketRepository {

    override suspend fun getCachedMarkets(): List<YieldMarketToken>? = withContext(dispatchers.io) {
        store.getSyncOrNull()?.enrichNetworkIds()
    }

    override suspend fun updateMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val response = yieldSupplyApi.getYieldMarkets().getOrThrow()
        val domain = response.marketDtos.map(YieldMarketTokenConverter::convert)
        store.store(domain)
        domain
    }

    override fun getMarketsFlow(): Flow<List<YieldMarketToken>> = store.get().map {
        it.enrichNetworkIds()
    }

    override suspend fun getTokenStatus(cryptoCurrencyToken: CryptoCurrency.Token): YieldMarketTokenStatus {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.backendId)?.getChainId()
            ?: error("Chain id is required for evm's")
        val response = yieldSupplyApi.getYieldTokenStatus(chainId, cryptoCurrencyToken.contractAddress).getOrThrow()
        return YieldTokenStatusConverter.convert(response)
    }

    override suspend fun getTokenChart(cryptoCurrencyToken: CryptoCurrency.Token): YieldSupplyMarketChartData {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.backendId)?.getChainId()
            ?: error("Chain id is required for evm's")
        val response = yieldSupplyApi.getYieldTokenChart(chainId, cryptoCurrencyToken.contractAddress).getOrThrow()
        return YieldTokenChartConverter.convert(response)
    }

    private fun List<YieldMarketToken>.enrichNetworkIds(): List<YieldMarketToken> {
        val chainIdMap = Blockchain.entries.associate { it.getChainId() to it.toNetworkId() }
        return this.map { token ->
            token.copy(backendId = chainIdMap[token.chainId])
        }
    }
}