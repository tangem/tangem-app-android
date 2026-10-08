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
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.grow.datasource.yield.YieldIncentivesApi
import com.tangem.grow.datasource.yield.YieldSupplyApi
import com.tangem.grow.datasource.yield.models.YieldSupplyChangeTokenStatusBody
import com.tangem.grow.datasource.yield.models.YieldSupplyMarketTokenDto
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.grow.datasource.preferences.GrowPreferencesKeys
import com.tangem.datasource.local.preferences.utils.get
import com.tangem.datasource.local.preferences.utils.store
import com.tangem.grow.datasource.yield.local.YieldMarketsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.models.YieldMarketToken
import com.tangem.domain.yield.supply.models.YieldSupplyPendingStatus
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class DefaultYieldSupplyRepository(
    private val yieldSupplyApi: YieldSupplyApi,
    private val yieldIncentivesApi: YieldIncentivesApi,
    private val store: YieldMarketsStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
    private val appPreferencesStore: AppPreferencesStore,
    private val growPreferencesKeys: GrowPreferencesKeys,
    private val isMerklIncentivesEnabled: Boolean,
) : YieldSupplyRepository {

    private val statusMapFlow = MutableStateFlow<Map<String, YieldSupplyPendingStatus>>(emptyMap())

    override suspend fun getCachedMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val cache = store.getSyncOrNull().orEmpty()
        val domain = cache.map(YieldMarketTokenConverter::convert)
        domain.enrichNetworkIds()
    }

    override suspend fun updateMarkets(): List<YieldMarketToken> = withContext(dispatchers.io) {
        val chains = Blockchain.yieldSupplySupportedBlockchains().map { it.getChainId() }.joinToString(",")
        val marketDtos = getIncentivisedMarkets(chains)
            ?: yieldSupplyApi.getYieldMarkets(chainId = chains).getOrThrow().marketDtos
        store.store(marketDtos)
        marketDtos.map(YieldMarketTokenConverter::convert)
    }

    /**
     * The incentivised markets payload is a strict superset of the plain one, so the whole incentive overlay is a URL
     * switch. Null → fall back to the plain markets.
     *
     * An empty payload is treated as an absence too: the incentives service may not know a chain the plain markets
     * still serve, and overwriting the cache with it would hide Yield Mode entirely.
     */
    private suspend fun getIncentivisedMarkets(chains: String): List<YieldSupplyMarketTokenDto>? {
        if (!isMerklIncentivesEnabled) return null

        return when (val response = yieldIncentivesApi.getIncentiveMarkets(chainId = chains)) {
            is ApiResponse.Success -> response.data.marketDtos.takeIf(List<YieldSupplyMarketTokenDto>::isNotEmpty)
            is ApiResponse.Error -> {
                // A 400 means the chain is not configured for incentives yet — an expected answer, not a failure.
                val code = (response.cause as? ApiResponseError.HttpException)?.code
                if (code != ApiResponseError.HttpException.Code.BAD_REQUEST) {
                    TangemLogger.e("Incentivised yield markets unavailable, using the plain markets", response.cause)
                }
                null
            }
        }
    }

    override fun getMarketsFlow(): Flow<List<YieldMarketToken>> = store.get().map {
        it.map(YieldMarketTokenConverter::convert).enrichNetworkIds()
    }

    override suspend fun getTokenStatus(cryptoCurrencyToken: CryptoCurrency.Token): YieldMarketToken {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.rawId)?.getChainId()
            ?: error("Chain id is required for evm's")
        val response = yieldSupplyApi.getYieldTokenStatus(chainId, cryptoCurrencyToken.contractAddress).getOrThrow()
        return YieldMarketTokenConverter.convert(response)
    }

    override suspend fun getTokenChart(cryptoCurrencyToken: CryptoCurrency.Token): YieldSupplyMarketChartData {
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.rawId)?.getChainId()
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
        val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.rawId)?.getChainId()
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
            val chainId = Blockchain.fromNetworkId(cryptoCurrencyToken.network.rawId)?.getChainId()
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
        statusMapFlow.update { currentMap ->
            if (yieldSupplyPendingStatus != null) {
                currentMap + (key to yieldSupplyPendingStatus)
            } else {
                currentMap - key
            }
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
        return statusMapFlow.value[getTokenProtocolStatusKey(userWalletId, cryptoCurrency)]
    }

    override fun getTokenProtocolPendingStatusFlow(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
    ): Flow<YieldSupplyPendingStatus?> {
        val key = getTokenProtocolStatusKey(userWalletId, cryptoCurrency)
        return statusMapFlow.map { it[key] }
    }

    private fun List<YieldMarketToken>.enrichNetworkIds(): List<YieldMarketToken> {
        val chainIdMap = Blockchain.entries.associate { it.getChainId() to it.toNetworkId() }
        return this.map { token ->
            token.copy(backendId = chainIdMap[token.chainId])
        }
    }

    override fun getShouldShowYieldPromoBanner(): Flow<Boolean> {
        return appPreferencesStore.get(growPreferencesKeys.yieldSupplyShouldShowMainPromoKey, true)
    }

    override suspend fun setShouldShowYieldPromoBanner(shouldShow: Boolean) {
        appPreferencesStore.store(growPreferencesKeys.yieldSupplyShouldShowMainPromoKey, shouldShow)
    }

    private fun getTokenProtocolStatusKey(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency): String =
        "${userWalletId}_${cryptoCurrency.id.value}"
}