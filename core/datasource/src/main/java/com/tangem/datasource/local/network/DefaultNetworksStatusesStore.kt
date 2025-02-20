package com.tangem.datasource.local.network

import androidx.datastore.core.DataStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.converter.NetworkStatusConverter
import com.tangem.datasource.local.network.converter.NetworkStatusDataModelConverter
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private typealias NetworkStatusesByWalletId = Map<String, Set<NetworkStatusDM>>

internal class DefaultNetworksStatusesStore(
    private val runtimeDataStore: RuntimeDataStore<Set<NetworkStatus>>,
    private val persistenceDataStore: DataStore<NetworkStatusesByWalletId>,
) : NetworksStatusesStore {

    override fun get(key: UserWalletId): Flow<Set<NetworkStatus>> {
        return runtimeDataStore.get(provideStringKey(key))
    }

    override fun get(key: UserWalletId, networks: Set<Network>): Flow<Set<NetworkStatus>> = channelFlow {
        val cachedStatuses = persistenceDataStore.data.firstOrNull()
            ?.get(key.stringValue)
            ?.mapNotNullTo(mutableSetOf()) { status ->
                val network = networks
                    .firstOrNull { it.id == status.networkId }
                    ?: return@mapNotNullTo null

                NetworkStatusConverter(network = network, isCached = true).convert(value = status)
            }
            .orEmpty()

        if (cachedStatuses.isNotEmpty()) {
            send(cachedStatuses)

            /**
             * Required for storing cache data.
             * This will help to recognize networks that are still uploaded.
             *
             * @see mergeStatuses
             */
            storeAll(key = key, values = cachedStatuses)
        }

        runtimeDataStore.get(provideStringKey(key))
            .onEach { runtimeStatuses ->
                val mergedStatuses = mergeStatuses(
                    networks = networks,
                    cachedStatuses = cachedStatuses,
                    runtimeStatuses = runtimeStatuses,
                )

                send(mergedStatuses)
            }
            .launchIn(scope = this)
    }

    override suspend fun getSyncOrNull(key: UserWalletId): Set<NetworkStatus>? {
        return runtimeDataStore.getSyncOrNull(key = provideStringKey(key))
    }

    override suspend fun store(key: UserWalletId, value: NetworkStatus) {
        storeAll(key = key, values = setOf(value))
    }

    override suspend fun storeAll(key: UserWalletId, values: Set<NetworkStatus>) {
        coroutineScope {
            val updatedValues = getSyncOrNull(key).orEmpty()
                .addOrReplace(items = values) { prev, new -> prev.network == new.network }

            launch { runtimeDataStore.store(key = provideStringKey(key), value = updatedValues) }
            launch { storeNetworkStatusInPersistence(userWalletId = key, statuses = updatedValues) }
        }
    }

    /**
     * Merge [cachedStatuses] with [runtimeStatuses]
     * The resulting set contains statuses from both sets.
     * If a status with the same network is in both sets, the status from [runtimeStatuses] is used.
     */
    private fun mergeStatuses(
        networks: Set<Network>,
        cachedStatuses: Set<NetworkStatus>,
        runtimeStatuses: Set<NetworkStatus>,
    ): Set<NetworkStatus> {
        return networks.mapNotNullTo(hashSetOf()) { network ->
            val runtimeStatus = runtimeStatuses.firstOrNull { it.network == network }

            if (runtimeStatus == null || runtimeStatus.value is NetworkStatus.Unreachable) {
                getCachedStatusIfPossible(cachedStatuses = cachedStatuses, network = network)
                    ?: runtimeStatus
            } else {
                runtimeStatus
            }
        }
    }

    private fun getCachedStatusIfPossible(cachedStatuses: Set<NetworkStatus>, network: Network): NetworkStatus? {
        val cached = cachedStatuses.firstOrNull { it.network == network } ?: return null

        val updatedCachedStatus = when (val status = cached.value) {
            is NetworkStatus.NoAccount -> status.copy(source = StatusSource.ONLY_CACHE)
            is NetworkStatus.Verified -> status.copy(source = StatusSource.ONLY_CACHE)
            is NetworkStatus.Refreshing,
            is NetworkStatus.Unreachable,
            is NetworkStatus.MissedDerivation,
            -> null
        }

        return if (updatedCachedStatus != null) {
            cached.copy(value = updatedCachedStatus)
        } else {
            null
        }
    }

    private suspend fun storeNetworkStatusInPersistence(userWalletId: UserWalletId, statuses: Set<NetworkStatus>) {
        // Converter will return null if the network status is not supported
        val newStatuses = NetworkStatusDataModelConverter.convertSet(input = statuses).filterNotNull().toSet()

        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                this[userWalletId.stringValue] = newStatuses
            }
        }
    }

    private fun provideStringKey(key: UserWalletId): String {
        return "network_statuses_${key.stringValue}"
    }
}