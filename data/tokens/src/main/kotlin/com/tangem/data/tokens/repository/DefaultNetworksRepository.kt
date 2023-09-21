package com.tangem.data.tokens.repository

import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.data.tokens.utils.ResponseCryptoCurrenciesFactory
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
    private val cardCurrenciesFactory by lazy { CardCryptoCurrenciesFactory(demoConfig) }
    private val responseCurrenciesFactory by lazy { ResponseCryptoCurrenciesFactory(demoConfig) }
    private val networkStatusFactory by lazy { NetworkStatusFactory() }

    private val networksStatuses: MutableStateFlow<Set<NetworkStatus>> = MutableStateFlow(hashSetOf())

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        launch(dispatchers.io) {
            networksStatuses.collect(::send)
        }

        launch(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh = false)
        }
    }

    override suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ): Set<NetworkStatus> = withContext(dispatchers.io) {
        fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh)
        networksStatuses.first().toSet()
    }

    private suspend fun fetchNetworksStatusesIfCacheExpired(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ) {
        coroutineScope {
            networks
                .map { network ->
                    async {
                        fetchNetworkStatusIfCacheExpired(userWalletId, network, refresh)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun fetchNetworkStatusIfCacheExpired(
        userWalletId: UserWalletId,
        network: Network,
        refresh: Boolean,
    ) {
        cacheRegistry.invokeOnExpire(
            key = getNetworksStatusesCacheKey(userWalletId, network),
            skipCache = refresh,
            block = { fetchNetworkStatus(userWalletId, network) },
        )
    }

    private suspend fun fetchNetworkStatus(userWalletId: UserWalletId, network: Network) {
        val currencies = getCurrencies(userWalletId, network)

        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            network = network,
            extraTokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )

        val networkStatus = networkStatusFactory.createNetworkStatus(
            network = network,
            result = result,
            currencies = currencies.toSet(),
        )

        networksStatuses.update { statuses ->
            statuses.addOrReplace(networkStatus) { it.network == networkStatus.network }
        }

        invalidateCacheKeyIfNeeded(userWalletId, networkStatus)
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId, network: Network): Sequence<CryptoCurrency> {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }

        val currencies = if (userWallet.isMultiCurrency) {
            val response = requireNotNull(userTokensStore.getSyncOrNull(userWalletId)) {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            }

            responseCurrenciesFactory.createCurrencies(response, userWallet.scanResponse).asSequence()
        } else {
            val currency = cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)

            sequenceOf(currency)
        }

        return currencies.filter { it.network == network }
    }

    private suspend fun invalidateCacheKeyIfNeeded(userWalletId: UserWalletId, networkStatus: NetworkStatus) {
        when (networkStatus.value) {
            is NetworkStatus.Verified,
            is NetworkStatus.NoAccount,
            -> Unit
            is NetworkStatus.Unreachable,
            is NetworkStatus.MissedDerivation,
            -> cacheRegistry.invalidate(getNetworksStatusesCacheKey(userWalletId, networkStatus.network))
        }
    }

    private fun getNetworksStatusesCacheKey(userWalletId: UserWalletId, network: Network): String {
        return "network_status_${userWalletId}_${network.id}_${network.derivationPath.value}"
    }
}