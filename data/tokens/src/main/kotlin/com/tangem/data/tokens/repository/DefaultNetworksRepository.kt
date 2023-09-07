package com.tangem.data.tokens.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.CardCurrenciesFactory
import com.tangem.data.tokens.utils.NetworkConverter
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.data.tokens.utils.ResponseCurrenciesFactory
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

internal class DefaultNetworksRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensStore: UserTokensStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksRepository {

    private val demoConfig by lazy { DemoConfig() }
    private val networkConverter by lazy { NetworkConverter() }
    private val cardCurrenciesFactory by lazy { CardCurrenciesFactory(demoConfig) }
    private val responseCurrenciesFactory by lazy { ResponseCurrenciesFactory(demoConfig) }
    private val networkStatusFactory by lazy { NetworkStatusFactory() }

    private val networksStatuses: MutableStateFlow<List<NetworkStatus>> = MutableStateFlow(emptyList())

    override fun getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return networkConverter.convertSet(networksIds)
    }

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        launch(dispatchers.io) {
            networksStatuses.collect {
                send(it.toSet())
            }
        }

        launch(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh = false)
        }
    }

    override suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ): Set<NetworkStatus> = withContext(dispatchers.io) {
        fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh)
        networksStatuses.first().toSet()
    }

    private suspend fun fetchNetworksStatusesIfCacheExpired(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ) {
        coroutineScope {
            networks
                .map { networkId ->
                    async {
                        fetchNetworkStatusIfCacheExpired(userWalletId, networkId, refresh)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun fetchNetworkStatusIfCacheExpired(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        refresh: Boolean,
    ) {
        cacheRegistry.invokeOnExpire(
            key = getNetworksStatusesCacheKey(userWalletId, networkId),
            skipCache = refresh,
            block = { fetchNetworkStatus(userWalletId, networkId) },
        )
    }

    private suspend fun fetchNetworkStatus(userWalletId: UserWalletId, networkId: Network.ID) {
        val currencies = getCurrencies(userWalletId)
            .asSequence()
            .filter { it.network.id == networkId }

        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            networkId = networkId,
            extraTokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )

        // Invalidate cache key if wallet manager update failed
        when (result) {
            is UpdateWalletManagerResult.Verified,
            is UpdateWalletManagerResult.NoAccount,
            -> Unit
            is UpdateWalletManagerResult.Unreachable,
            is UpdateWalletManagerResult.MissedDerivation,
            -> cacheRegistry.invalidate(getNetworksStatusesCacheKey(userWalletId, networkId))
        }

        val networkStatus = networkStatusFactory.createNetworkStatus(
            networkId = networkId,
            result = result,
            currencies = currencies.toSet(),
        )

        networksStatuses.update { statuses ->
            statuses.addOrReplace(networkStatus) { it.networkId == networkStatus.networkId }
        }
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId): List<CryptoCurrency> {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }

        return if (userWallet.isMultiCurrency) {
            val response = requireNotNull(userTokensStore.getSyncOrNull(userWalletId)) {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            }

            responseCurrenciesFactory.createCurrencies(response, userWallet.scanResponse.card)
        } else {
            val currency = cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)

            listOf(currency)
        }
    }

    private fun getNetworksStatusesCacheKey(userWalletId: UserWalletId, nerworkId: Network.ID): String {
        return "network_status_${userWalletId}_${nerworkId.value}"
    }
}