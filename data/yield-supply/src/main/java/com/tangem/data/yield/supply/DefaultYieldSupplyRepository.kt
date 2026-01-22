package com.tangem.data.yield.supply

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.TransactionStatus
import com.tangem.blockchain.yieldsupply.YieldSupplyProvider
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.core.analytics.models.ExceptionAnalyticsEvent
import com.tangem.data.yield.supply.converters.YieldMarketTokenConverter
import com.tangem.data.yield.supply.converters.YieldTokenChartConverter
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.YieldSupplyApi
import com.tangem.datasource.api.tangemTech.models.YieldSupplyChangeTokenStatusBody
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.datasource.local.yieldsupply.YieldMarketsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

internal class DefaultYieldSupplyRepository(
    private val yieldSupplyApi: YieldSupplyApi,
    private val store: YieldMarketsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val appPreferencesStore: AppPreferencesStore,
) : YieldSupplyRepository {

    private val statusMap: MutableMap<String, YieldSupplyPendingStatus> = ConcurrentHashMap()

    override suspend fun getCachedMarkets(): List<YieldMarketToken>? = withContext(dispatchers.io) {
        val cache = store.getSyncOrNull().orEmpty()
        val domain = cache.map(YieldMarketTokenConverter::convert)
        domain.enrichNetworkIds()
    }

    override suspend fun updateMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val chains = Blockchain.yieldSupplySupportedBlockchains().map { it.getChainId() }.joinToString(",")
        val response = yieldSupplyApi.getYieldMarkets(chainId = chains).getOrThrow()
        val domain = response.marketDtos.map(YieldMarketTokenConverter::convert)
        store.store(response.marketDtos)
        domain
    }

    override fun getMarketsFlow(): Flow<List<YieldMarketToken>> = store.get().map {
        it.map(YieldMarketTokenConverter::convert).enrichNetworkIds()
    }

    override suspend fun getTokenStatus(cryptoCurrencyToken: CryptoCurrency.Token): YieldMarketToken {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.backendId)?.getChainId()
            ?: error("Chain id is required for evm's")
        val response = yieldSupplyApi.getYieldTokenStatus(chainId, cryptoCurrencyToken.contractAddress).getOrThrow()
        return YieldMarketTokenConverter.convert(response)
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
            )
            if (walletManager == null) {
                analyticsExceptionHandler.sendException(
                    ExceptionAnalyticsEvent(
                        exception = IllegalStateException("Wallet manager not found"),
                    ),
                )
                return@withContext false
            }

            (walletManager as? YieldSupplyProvider)?.isSupported() ?: false
        }

    override suspend fun activateProtocol(
        userWalletId: UserWalletId,
        cryptoCurrencyToken: CryptoCurrency.Token,
        address: String,
    ): Boolean = withContext(dispatchers.io) {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.backendId)?.getChainId()
            ?: error("Chain id is required for evm's")
        yieldSupplyApi.activateYieldModule(
            body = YieldSupplyChangeTokenStatusBody(
                tokenAddress = cryptoCurrencyToken.contractAddress,
                chainId = chainId,
                userAddress = address,
            ),
            userWalletId = userWalletId.stringValue,
        ).getOrThrow().isActive
    }

    override suspend fun deactivateProtocol(cryptoCurrencyToken: CryptoCurrency.Token, address: String): Boolean =
        withContext(dispatchers.io) {
            val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.backendId)?.getChainId()
                ?: error("Chain id is required for evm's")
            yieldSupplyApi.deactivateYieldModule(
                YieldSupplyChangeTokenStatusBody(
                    tokenAddress = cryptoCurrencyToken.contractAddress,
                    chainId = chainId,
                    userAddress = address,
                ),
            ).getOrThrow().isActive
        }

    override suspend fun saveTokenProtocolPendingStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        yieldSupplyPendingStatus: YieldSupplyPendingStatus?,
    ) {
        val key = getTokenProtocolStatusKey(userWalletId, cryptoCurrency)
        if (yieldSupplyPendingStatus != null) {
            statusMap[key] = yieldSupplyPendingStatus
        } else {
            statusMap.remove(key)
        }
    }

    override suspend fun getPendingTxHashes(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): List<String> {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWalletId,
            network = cryptoCurrency.network,
        ) ?: return emptyList()

        return walletManager.wallet.recentTransactions
            .filter { it.status == TransactionStatus.Unconfirmed }
            .map {
                it.hash.orEmpty()
            }
    }

    override fun getTokenProtocolPendingStatus(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): YieldSupplyPendingStatus? {
        return statusMap[getTokenProtocolStatusKey(userWalletId, cryptoCurrency)]
    }

    private fun List<YieldMarketToken>.enrichNetworkIds(): List<YieldMarketToken> {
        val chainIdMap = Blockchain.entries.associate { it.getChainId() to it.toNetworkId() }
        return this.map { token ->
            token.copy(backendId = chainIdMap[token.chainId])
        }
    }

    override fun getShouldShowYieldPromoBanner(): Flow<Boolean> {
        return appPreferencesStore.get(PreferencesKeys.YIELD_SUPPLY_SHOULD_SHOW_MAIN_PROMO_KEY, true)
    }

    override suspend fun setShouldShowYieldPromoBanner(shouldShow: Boolean) {
        appPreferencesStore.store(PreferencesKeys.YIELD_SUPPLY_SHOULD_SHOW_MAIN_PROMO_KEY, shouldShow)
    }

    private fun getTokenProtocolStatusKey(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String =
        "${userWalletId}_${cryptoCurrency.id.value}"
}