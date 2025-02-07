package com.tangem.data.tokens.repository

import arrow.fx.coroutines.parZip
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.address.AddressType
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.common.cache.CacheRegistry
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.data.tokens.entity.NetworkStatusDM
import com.tangem.data.tokens.utils.CardCryptoCurrenciesFactory
import com.tangem.data.tokens.utils.NetworkStatusFactory
import com.tangem.data.tokens.utils.NetworkStatusMapper
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.datasource.local.network.NetworksStatusesStore
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSetSync
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAddress
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.walletmanager.model.UpdateWalletManagerResult
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Suppress("LongParameterList")
internal class DefaultNetworksRepository(
    private val networksStatusesStore: NetworksStatusesStore,
    private val walletManagersFacade: WalletManagersFacade,
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
    private val cacheRegistry: CacheRegistry,
    private val dispatchers: CoroutineDispatcherProvider,
    excludedBlockchains: ExcludedBlockchains,
) : NetworksRepository {

    private val demoConfig = DemoConfig()
    private val cardCurrenciesFactory = CardCryptoCurrenciesFactory(demoConfig, excludedBlockchains)
    private val responseCurrenciesFactory = ResponseCryptoCurrenciesFactory(excludedBlockchains)
    private val networkStatusFactory = NetworkStatusFactory()

    override fun getNetworkStatusesUpdates(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        val persistenceStatuses = getNetworkStatusesFromPersistenceStore(userWalletId, networks)

        if (persistenceStatuses.isNotEmpty()) {
            send(persistenceStatuses)
        }

        networksStatusesStore.get(userWalletId)
            .onEach { runtimeStatuses ->
                if (persistenceStatuses.isEmpty()) {
                    send(runtimeStatuses)
                } else {
                    val mergedStatuses = persistenceStatuses.toMutableList()

                    runtimeStatuses.forEach { runtimeStatus ->
                        val index = mergedStatuses.indexOfFirst { it.network == runtimeStatus.network }
                        if (index != -1) {
                            mergedStatuses[index] = runtimeStatus
                        } else {
                            mergedStatuses.add(runtimeStatus)
                        }
                    }

                    send(mergedStatuses.toSet())
                }
            }
            .launchIn(scope = this + dispatchers.io)
    }

    private suspend fun getNetworkStatusesFromPersistenceStore(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Set<NetworkStatus> {
        val (statuses, currencies) = parZip(
            {
                appPreferencesStore.getObjectSetSync<NetworkStatusDM>(
                    key = PreferencesKeys.getNetworkStatusesKey(userWalletId),
                )
            },
            {
                getCurrencies(userWalletId, networks)
            },
            { statuses, currencies -> statuses to currencies },
        )

        return statuses.mapNotNullTo(mutableSetOf()) { status ->
            val network = networks
                .firstOrNull { it.id == status.networkId }
                ?: return@mapNotNullTo null

            NetworkStatusMapper.toDomainModel(network, currencies, status)
        }
    }

    override suspend fun fetchNetworkStatuses(userWalletId: UserWalletId, networks: Set<Network>, refresh: Boolean) {
        withContext(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh)
        }
    }

    override fun getNetworkStatusesUpdatesLegacy(
        userWalletId: UserWalletId,
        networks: Set<Network>,
    ): Flow<Set<NetworkStatus>> = channelFlow {
        networksStatusesStore.get(userWalletId)
            .onEach(::send)
            .launchIn(scope = this + dispatchers.io)

        withContext(dispatchers.io) {
            fetchNetworksStatusesIfCacheExpired(userWalletId, networks, refresh = false)
        }
    }

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
        return REQUIRED_ACCOUNT_WITHOUT_RESERVE_BLOCKCHAINS.contains(blockchain)
    }

    override suspend fun getNetworkAddresses(
        userWalletId: UserWalletId,
        network: Network,
    ): List<CryptoCurrencyAddress> = withContext(dispatchers.io) {
        // Get list of currencies matching [network]
        val currencies = getCurrencies(userWalletId)
            .filter { currency -> network.id == currency.network.id }

        // There is no currencies matching given [networks] in [userWalletId]
        if (currencies.toList().isEmpty()) return@withContext emptyList()

        currencies.toList().map { currency ->
            CryptoCurrencyAddress(
                cryptoCurrency = currency,
                address = walletManagersFacade.getAddresses(userWalletId, currency.network)
                    .firstOrNull { it.type == AddressType.Default }
                    ?.value.orEmpty(),
            )
        }
    }

    private suspend fun fetchNetworksStatusesIfCacheExpired(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        refresh: Boolean,
    ) = coroutineScope {
        if (refresh) {
            val statusesToRefresh = networks.map { NetworkStatus(it, NetworkStatus.Refreshing) }
            networksStatusesStore.storeAll(userWalletId, statusesToRefresh)
        }

        val currencies = getCurrencies(userWalletId, networks)
        val networksDeferred = networks.mapNotNull { network ->
            coroutineScope {
                val key = getNetworksStatusesCacheKey(userWalletId, network)

                if (refresh || cacheRegistry.isExpired(key)) {
                    async {
                        cacheRegistry.invokeOnExpire(
                            key = key,
                            skipCache = refresh,
                            block = { fetchNetworkStatus(userWalletId, network, currencies) },
                        )
                    }
                } else {
                    null
                }
            }
        }

        networksDeferred.awaitAll()
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

    private suspend fun fetchNetworkStatus(
        userWalletId: UserWalletId,
        network: Network,
        currencies: Sequence<CryptoCurrency>,
    ) {
        val networkCurrencies = currencies.filter { it.network == network }

        val result = walletManagersFacade.update(
            userWalletId = userWalletId,
            network = network,
            extraTokens = networkCurrencies
                .filterIsInstance<CryptoCurrency.Token>()
                .toSet(),
        )

        withContext(NonCancellable) {
            invalidateCacheKeyIfNeeded(userWalletId, network, result)
        }

        val networkStatus = networkStatusFactory.createNetworkStatus(
            network = network,
            result = result,
            currencies = networkCurrencies.toSet(),
        )

        networksStatusesStore.store(userWalletId, networkStatus)
        storeNetworkStatusInPersistence(userWalletId, networkStatus)
    }

    private suspend fun storeNetworkStatusInPersistence(userWalletId: UserWalletId, networkStatus: NetworkStatus) {
        val status = networkStatus.value
        val network = networkStatus.network

        if (status !is NetworkStatus.Verified) return

        val key = PreferencesKeys.getNetworkStatusesKey(userWalletId)
        appPreferencesStore.editData { preferences ->
            val storedStatuses = preferences.getObjectSet<NetworkStatusDM>(key) ?: emptySet()
            val updatedStatuses = storedStatuses.addOrReplace(NetworkStatusMapper.toDataModel(network, status)) {
                it.networkId == network.id
            }

            preferences.setObjectSet(key, updatedStatuses)
        }
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
        val currencies = getCurrencies(userWalletId)
        return currencies.filter { it.network in networks }
    }

    private suspend fun getCurrencies(userWalletId: UserWalletId): Sequence<CryptoCurrency> {
        val userWallet = requireNotNull(userWalletsStore.getSyncOrNull(userWalletId)) {
            "Unable to find user wallet with provided ID: $userWalletId"
        }

        return if (userWallet.isMultiCurrency) {
            val response = requireNotNull(
                value = appPreferencesStore.getObjectSyncOrNull<UserTokensResponse>(
                    key = PreferencesKeys.getUserTokensKey(userWalletId.stringValue),
                ),
                lazyMessage = {
                    "Unable to find tokens response for user wallet with provided ID: $userWalletId"
                },
            )

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

    private companion object {

        val REQUIRED_ACCOUNT_WITHOUT_RESERVE_BLOCKCHAINS = listOf(Blockchain.Aptos, Blockchain.Filecoin)
    }
}