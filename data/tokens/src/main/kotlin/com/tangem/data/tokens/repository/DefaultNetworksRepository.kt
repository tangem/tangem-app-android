package com.tangem.data.tokens.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.data.tokens.utils.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.local.network.NetworksStatusesStore
import com.tangem.datasource.local.token.UserTokensStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

internal class DefaultNetworksRepository(
    private val networksStatusesStore: NetworksStatusesStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensStore: UserTokensStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworksRepository {

    private val demoConfig by lazy { DemoConfig() }
    private val cardCurrenciesFactory by lazy { CardCryptoCurrenciesFactory(demoConfig) }
    private val responseCurrenciesFactory by lazy { ResponseCryptoCurrenciesFactory() }
    private val networkStatusFactory by lazy { NetworkStatusFactory() }

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        launch(dispatchers.io) {
            networksStatusesStore.get(userWalletId)
                .collectLatest(::send)
        }

        withContext(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, false)
        }
    }
        .cancellable()

    override suspend fun fetchNetworkPendingTransactions(userWalletId: UserWalletId, networks: Set<Network>) {
        val currencies = getCurrencies(userWalletId, networks)
        withContext(dispatchers.io) {
            fetchNetworksPendingTransactions(userWalletId, networks, currencies)
        }
    }

    override suspend fun getNetworkStatusesSync(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ): Set<NetworkStatus> = withContext(dispatchers.io) {
        fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh)
        networksStatusesStore.getSyncOrNull(userWalletId).orEmpty()
    }

    override fun isNeedToCreateAccountWithoutReserve(network: Network): Boolean {
        val blockchain = Blockchain.fromNetworkId(network.id.value)
        return blockchain == Blockchain.Aptos
    }

    private suspend fun fetchNetworksStatusesIfCacheExpired(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ) {
        val currencies = getCurrencies(userWalletId, networks)
        coroutineScope {
            networks
                .map { network ->
                    async {
                        fetchNetworkStatusIfCacheExpired(userWalletId, network, currencies, refresh)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun fetchNetworksPendingTransactions(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        currencies: Sequence<CryptoCurrency>,
    ) {
        coroutineScope {
            networks
                .map { network ->
                    async {
                        fetchNetworkPendingTransactions(userWalletId, network, currencies)
                    }
                }
                .awaitAll()
        }
    }

    private suspend fun fetchNetworkStatusIfCacheExpired(
        userWalletId: UserWalletId,
        network: Network,
        currencies: Sequence<CryptoCurrency>,
        refresh: Boolean,
    ) {
        cacheRegistry.invokeOnExpire(
            key = getNetworksStatusesCacheKey(userWalletId, network),
            skipCache = refresh,
            block = { fetchNetworkStatus(userWalletId, network, currencies) },
        )
    }

    private suspend fun fetchNetworkStatus(
        userWalletId: UserWalletId,
        network: Network,
        currencies: Sequence<CryptoCurrency>,
    ) {
        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            network = network,
            extraTokens = currencies
                .filterIsInstance<CryptoCurrency.Token>()
                .filter { it.network == network }
                .toSet(),
        )

        withContext(NonCancellable) {
            invalidateCacheKeyIfNeeded(userWalletId, network, result)
        }

        val networkStatus = networkStatusFactory.createNetworkStatus(
            network = network,
            result = result,
            currencies = currencies.toSet(),
        )

        networksStatusesStore.store(userWalletId, networkStatus)
    }

    private suspend fun fetchNetworkPendingTransactions(
        userWalletId: UserWalletId,
        network: Network,
        currencies: Sequence<CryptoCurrency>,
    ) {
        val result = walletManagersFacade.updatePendingTransactions(
            userWalletId = userWalletId,
            network = network,
        )

        withContext(NonCancellable) {
            invalidateCacheKeyIfNeeded(userWalletId, network, result)
        }

        val networkStatus = networkStatusFactory.createNetworkStatus(
            network = network,
            result = result,
            currencies = currencies.toSet(),
        )

        networksStatusesStore.store(userWalletId, networkStatus)
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId, networks: Set<Network>): Sequence<CryptoCurrency> {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }

        val currencies = if (userWallet.isMultiCurrency) {
            val response = requireNotNull(userTokensStore.getSyncOrNull(userWalletId)) {
                "Unable to find tokens response for user wallet with provided ID: $userWalletId"
            }

            responseCurrenciesFactory.createCurrencies(response, userWallet.scanResponse).asSequence()
        } else {
            if (userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()) {
                cardCurrenciesFactory.createCurrenciesForSingleCurrencyCardWithToken(userWallet.scanResponse)
                    .asSequence()
            } else {
                val currency = cardCurrenciesFactory.createPrimaryCurrencyForSingleCurrencyCard(userWallet.scanResponse)

                sequenceOf(currency)
            }
        }

        return currencies.filter { networks.contains(it.network) }
    }

    private suspend fun invalidateCacheKeyIfNeeded(
        userWalletId: UserWalletId,
        network: Network,
        result: UpdateWalletManagerResult,
    ) {
        when (result) {
            is UpdateWalletManagerResult.Verified,
            is UpdateWalletManagerResult.NoAccount,
            -> Unit
            is UpdateWalletManagerResult.Unreachable,
            is UpdateWalletManagerResult.MissedDerivation,
            -> {
                Timber.w(
                    """
                        Invalidate network cache key
                        |- User wallet ID: $userWalletId
                        |- Network: ${network.id}
                    """.trimIndent(),
                )

                cacheRegistry.invalidate(getNetworksStatusesCacheKey(userWalletId, network))
            }
        }
    }

    private fun getNetworksStatusesCacheKey(userWalletId: UserWalletId, network: Network): String {
        return "network_status_${userWalletId}_${network.id.value}_${network.derivationPath.value}"
    }
}
