package com.tangem.data.tokens.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.NetworkConverter
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.data.tokens.utils.ResponseCurrenciesFactory
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.models.Network
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class DefaultNetworksRepository(
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensStore: UserTokensStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksRepository {

    private val networkConverter by lazy { NetworkConverter() }
    private val responseCurrenciesFactory by lazy { ResponseCurrenciesFactory(DemoConfig()) }
    private val networkStatusFactory by lazy { NetworkStatusFactory() }

    private val networksStatuses: MutableStateFlow<HashSet<NetworkStatus>> = MutableStateFlow(hashSetOf())

    override fun getNetworks(networksIds: Set<Network.ID>): Set<Network> {
        return networkConverter.convertSet(networksIds)
    }

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        networksStatuses.collectLatest(::send)

        launch(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh)
        }
    }

    private suspend fun fetchNetworksStatusesIfCacheExpired(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ) {
        cacheRegistry.invokeOnExpire(
            key = getNetworksStatusesCacheKey(userWalletId),
            skipCache = refresh,
            block = { fetchNetworksStatuses(userWalletId, networks) },
        )
    }

    private suspend fun fetchNetworksStatuses(userWalletId: UserWalletId, networks: Set<Network.ID>) {
        coroutineScope {
            networks
                .map { networkId ->
                    async {
                        fetchNetworkStatus(userWalletId, networkId)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun fetchNetworkStatus(userWalletId: UserWalletId, networkId: Network.ID) {
        val currencies = getCurrencies(userWalletId)
        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            networkId = networkId,
            extraTokens = currencies.filterIsInstanceTo(hashSetOf()),
        )
        val networkStatus = networkStatusFactory.createNetworkStatus(networkId, result, currencies)

        networksStatuses.update { statuses ->
            statuses.apply {
                addOrReplace(networkStatus) { it.networkId == networkStatus.networkId }
            }
        }
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId): Set<CryptoCurrency> {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }
        val response = requireNotNull(userTokensStore.getSyncOrNull(userWalletId)) {
            "Unable to find tokens response for user wallet with provided ID: $userWalletId"
        }

        return responseCurrenciesFactory.createTokens(response, userWallet.scanResponse.card)
    }

    private fun getNetworksStatusesCacheKey(userWalletId: UserWalletId): String = "network_status_$userWalletId"
}
