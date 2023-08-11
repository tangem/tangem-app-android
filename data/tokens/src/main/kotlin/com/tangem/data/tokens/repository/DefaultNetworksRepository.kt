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
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    override fun getNetworkStatuses(
        userWalletId: UserWalletId,
        networks: Set<Network.ID>,
        refresh: Boolean,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        launch(dispatchers.io) {
            networksStatuses.collect {
                send(it.toSet())
            }
        }

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
            .asSequence()
            .filter { it.networkId == networkId }

        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            networkId = networkId,
            extraTokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )
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

    private fun getNetworksStatusesCacheKey(userWalletId: UserWalletId): String = "network_status_$userWalletId"
}