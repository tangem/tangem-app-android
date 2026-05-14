package com.tangem.data.assetsdiscovery.repository

import com.tangem.blockchain.assetsdiscovery.models.DiscoveredAsset
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.data.assetsdiscovery.store.AssetsDiscoveryStore
import com.tangem.data.assetsdiscovery.store.AssetsDiscoveryStoreFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.common.network.NetworkFactory
import com.tangem.datasource.api.common.response.getOrThrow
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectMapSync
import com.tangem.domain.assetsdiscovery.AssetsDiscoveryFacade
import com.tangem.domain.assetsdiscovery.model.AssetsDiscoveryProgress
import com.tangem.domain.assetsdiscovery.repository.AssetsDiscoveryRepository
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongParameterList")
internal class DefaultAssetsDiscoveryRepository(
    private val assetsDiscoveryFacade: AssetsDiscoveryFacade,
    private val tangemTechApi: TangemTechApi,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val networkFactory: NetworkFactory,
    private val appPreferencesStore: AppPreferencesStore,
    private val assetsDiscoveryStoreFactory: AssetsDiscoveryStoreFactory,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val dispatchers: CoroutineDispatcherProvider,
    private val excludedBlockchains: ExcludedBlockchains,
) : AssetsDiscoveryRepository {

    private val progressStates = ConcurrentHashMap<String, MutableStateFlow<AssetsDiscoveryProgress>>()

    override fun observeDiscoveryProgress(userWalletId: UserWalletId): Flow<AssetsDiscoveryProgress> {
        return getProgressFlow(userWalletId)
    }

    override fun acknowledgeCompletion(userWalletId: UserWalletId) {
        val key = userWalletId.stringValue
        val stateFlow = progressStates[key] ?: return
        stateFlow.value = AssetsDiscoveryProgress.Idle
        progressStates.remove(key, stateFlow)
    }

    override suspend fun getDiscoveredCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        val assetsDiscoveryStore = assetsDiscoveryStoreFactory.provide(userWalletId)
        val storedTokens = assetsDiscoveryStore.get()
        if (storedTokens.isEmpty()) return emptyList()

        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)
        return responseCryptoCurrenciesFactory.createCurrencies(
            tokens = storedTokens,
            userWallet = userWallet,
            accountIndex = DerivationIndex.Main,
        )
    }

    override suspend fun clearDiscoveredTokens(userWalletId: UserWalletId) {
        val assetsDiscoveryStore = assetsDiscoveryStoreFactory.provide(userWalletId)
        assetsDiscoveryStore.clear()
    }

    override suspend fun clearPendingFlag(userWalletId: UserWalletId) {
        setPendingFlag(userWalletId, value = false)
    }

    override suspend fun getPendingDiscoveryWalletIds(): List<UserWalletId> {
        val pendingMap = appPreferencesStore
            .getObjectMapSync<Boolean>(PreferencesKeys.PENDING_ASSETS_DISCOVERY_KEY)
        return pendingMap
            .filter { it.value }
            .map { UserWalletId(it.key) }
    }

    override suspend fun runDiscovery(userWalletId: UserWalletId) {
        val networks = getSupportedNetworks(userWalletId)

        if (networks.isEmpty()) return

        setPendingFlag(userWalletId, value = true)
        val assetsDiscoveryStore = assetsDiscoveryStoreFactory.provide(userWalletId)
        assetsDiscoveryStore.clear()

        val batches = networks.chunked(MAX_CONCURRENT_REQUESTS)
        var completedNetworks = 0
        getProgressFlow(userWalletId).value = AssetsDiscoveryProgress.InProgress(
            completedNetworks = 0,
            totalNetworks = networks.size,
        )

        for (batch in batches) {
            val batchResults = processBatch(userWalletId, batch)
            completedNetworks = handleBatchResults(
                userWalletId = userWalletId,
                results = batchResults,
                assetsDiscoveryStore = assetsDiscoveryStore,
                completedNetworks = completedNetworks,
                totalNetworks = networks.size,
            )
        }
    }

    override suspend fun completeDiscovery(userWalletId: UserWalletId) {
        setPendingFlag(userWalletId, value = false)
        getProgressFlow(userWalletId).value = AssetsDiscoveryProgress.Completed
    }

    private suspend fun processBatch(userWalletId: UserWalletId, batch: List<Network>): List<NetworkResult> {
        return coroutineScope {
            batch.map { network ->
                async(dispatchers.io) {
                    processNetwork(userWalletId, network)
                }
            }.awaitAll()
        }
    }

    private suspend fun handleBatchResults(
        userWalletId: UserWalletId,
        results: List<NetworkResult>,
        assetsDiscoveryStore: AssetsDiscoveryStore,
        completedNetworks: Int,
        totalNetworks: Int,
    ): Int {
        var completed = completedNetworks
        val progressFlow = getProgressFlow(userWalletId)

        for (result in results) {
            completed++
            handleNetworkResult(result, assetsDiscoveryStore)
            progressFlow.value = AssetsDiscoveryProgress.InProgress(
                completedNetworks = completed,
                totalNetworks = totalNetworks,
            )
        }

        return completed
    }

    private suspend fun handleNetworkResult(result: NetworkResult, assetsDiscoveryStore: AssetsDiscoveryStore) {
        when (result) {
            is NetworkResult.Success -> {
                if (result.responseTokens.isNotEmpty()) {
                    try {
                        assetsDiscoveryStore.append(result.responseTokens)
                    } catch (e: Exception) {
                        TangemLogger.e("Failed to store discovered tokens for network: ${result.networkId}", e)
                    }
                }
            }
            is NetworkResult.Error -> {
                TangemLogger.e("Assets discovery failed for network: ${result.networkId}", result.cause)
            }
        }
    }

    private suspend fun processNetwork(userWalletId: UserWalletId, network: Network): NetworkResult {
        return try {
            val discoveredAssets = discoverAndFilterAssets(userWalletId, network)

            if (discoveredAssets.isEmpty()) {
                return NetworkResult.Success(
                    networkId = network.rawId,
                    responseTokens = emptyList(),
                )
            }

            val enrichedTokens = enrichTokensWithCatalog(discoveredAssets, network)

            val responseTokens = enrichedTokens
                .filter { it.contractAddress != null }
                .map { it.toResponseToken() }

            NetworkResult.Success(
                networkId = network.rawId,
                responseTokens = responseTokens,
            )
        } catch (e: Exception) {
            NetworkResult.Error(networkId = network.rawId, cause = e)
        }
    }

    private suspend fun discoverAndFilterAssets(userWalletId: UserWalletId, network: Network): List<DiscoveredAsset> =
        withContext(dispatchers.io) {
            val providerInfo = assetsDiscoveryFacade.getAssetsDiscoveryService(userWalletId, network)
                ?: return@withContext emptyList()
            providerInfo.service.discoverAssets(providerInfo.address)
                .filter { it.amount > BigDecimal.ZERO }
        }

    private suspend fun enrichTokensWithCatalog(
        assets: List<DiscoveredAsset>,
        network: Network,
    ): List<EnrichedDiscoveredAsset> = withContext(dispatchers.io) {
        val tokensToEnrich = assets.filterIsInstance<DiscoveredAsset.Token>()

        val catalogMap = fetchCatalogInfo(
            networkId = network.rawId,
            contractAddresses = tokensToEnrich.map { it.contractAddress },
        )

        tokensToEnrich.mapNotNull { balance ->
            val contractAddressLower = balance.contractAddress.lowercase()
            val coin = catalogMap[contractAddressLower] ?: return@mapNotNull null
            val decimals = coin.networks
                .find { it.contractAddress?.lowercase() == contractAddressLower }
                ?.decimalCount
                ?.toInt()
                ?: 0

            EnrichedDiscoveredAsset(
                contractAddress = balance.contractAddress,
                symbol = coin.symbol,
                name = coin.name,
                decimals = decimals,
                amount = balance.amount,
                isNativeToken = false,
                currencyId = coin.id,
                networkId = network.rawId,
            )
        }
    }

    private suspend fun fetchCatalogInfo(
        networkId: String,
        contractAddresses: List<String>,
    ): Map<String, CoinsResponse.Coin> {
        if (contractAddresses.isEmpty()) return emptyMap()

        return try {
            val response = tangemTechApi.getCoins(
                networkId = networkId,
                contractAddresses = contractAddresses.joinToString(","),
                active = true,
            ).getOrThrow()

            buildMap {
                for (coin in response.coins) {
                    for (network in coin.networks) {
                        val address = network.contractAddress?.lowercase() ?: continue
                        put(address, coin)
                    }
                }
            }
        } catch (e: Exception) {
            TangemLogger.w(
                "Failed to fetch catalog info for networkId=$networkId, addresses=${contractAddresses.size}",
                e,
            )
            emptyMap()
        }
    }

    private fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> {
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

        return Blockchain.entries
            .filter { !it.isTestnet() }
            .filter { it !in excludedBlockchains }
            .mapNotNull { blockchain ->
                networkFactory.create(
                    blockchain = blockchain,
                    extraDerivationPath = null,
                    userWallet = userWallet,
                )
            }
    }

    private fun getProgressFlow(userWalletId: UserWalletId): MutableStateFlow<AssetsDiscoveryProgress> {
        return progressStates.getOrPut(userWalletId.stringValue) {
            MutableStateFlow(AssetsDiscoveryProgress.Idle)
        }
    }

    private suspend fun setPendingFlag(userWalletId: UserWalletId, value: Boolean) {
        appPreferencesStore.editData { prefs ->
            prefs.setObjectMap(
                key = PreferencesKeys.PENDING_ASSETS_DISCOVERY_KEY,
                value = prefs.getObjectMap<Boolean>(PreferencesKeys.PENDING_ASSETS_DISCOVERY_KEY)
                    .plus(userWalletId.stringValue to value),
            )
        }
    }

    private fun EnrichedDiscoveredAsset.toResponseToken(): UserTokensResponse.Token {
        return UserTokensResponse.Token(
            id = currencyId,
            networkId = networkId,
            name = name,
            symbol = symbol,
            decimals = decimals,
            contractAddress = contractAddress,
        )
    }

    private data class EnrichedDiscoveredAsset(
        val contractAddress: String?,
        val symbol: String,
        val name: String,
        val decimals: Int,
        val amount: BigDecimal,
        val isNativeToken: Boolean,
        val currencyId: String?,
        val networkId: String,
    )

    private sealed class NetworkResult {
        data class Success(
            val networkId: String,
            val responseTokens: List<UserTokensResponse.Token>,
        ) : NetworkResult()

        data class Error(
            val networkId: String,
            val cause: Throwable,
        ) : NetworkResult()
    }

    companion object {
        private const val MAX_CONCURRENT_REQUESTS = 3
    }
}