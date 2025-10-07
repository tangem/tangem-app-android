package com.tangem.data.yield.supply

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.data.yield.supply.converters.YieldTokenStatusConverter
import com.tangem.data.yield.supply.converters.YieldTokenChartConverter
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldMarketTokenStatus
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.collections.map

internal class DefaultYieldSupplyRepository(
    private val yieldSupplyApi: YieldSupplyApi,
    private val store: YieldMarketsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyRepository {

    override suspend fun getCachedMarkets(): List<YieldMarketToken>? = withContext(dispatchers.io) {
        val cache = store.getSyncOrNull().orEmpty()
        val domain = cache.map(YieldMarketTokenConverter::convert)
        domain.enrichNetworkIds()
    }

    override suspend fun updateMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val response = yieldSupplyApi.getYieldMarkets().getOrThrow()
        val domain = response.marketDtos.map(YieldMarketTokenConverter::convert)
        store.store(response.marketDtos)
        domain
    }

    override fun getMarketsFlow(): Flow<List<YieldMarketToken>> = store.get().map {
        it.map(YieldMarketTokenConverter::convert).enrichNetworkIds()
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

    override suspend fun isYieldSupplySupported(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): Boolean =
        withContext(dispatchers.io) {
            val walletManager = walletManagersFacade.getOrCreateWalletManager(
                userWalletId = userWalletId,
                blockchain = cryptoCurrency.network.toBlockchain(),
                derivationPath = cryptoCurrency.network.derivationPath.value,
            ) ?: error("Wallet manager not found")

            (walletManager as? YieldSupplyProvider)?.isSupported() ?: false
        }

    private fun List<YieldMarketToken>.enrichNetworkIds(): List<YieldMarketToken> {
        val chainIdMap = Blockchain.entries.associate { it.getChainId() to it.toNetworkId() }
        return this.map { token ->
            token.copy(backendId = chainIdMap[token.chainId])
        }
    }
}