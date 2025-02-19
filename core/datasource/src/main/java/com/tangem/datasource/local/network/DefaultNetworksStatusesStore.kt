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
import com.tangem.utils.extensions.replaceBy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private typealias NetworkStatusesByWalletId = Map<String, Set<NetworkStatusDM>>

internal class DefaultNetworksStatusesStore(
    private val runtimeDataStore: RuntimeDataStore<Set<NetworkStatus>>,
    private val persistenceDataStore: DataStore<NetworkStatusesByWalletId>,
) : NetworksStatusesStore {

    private val mutex = Mutex()

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
        }

        runtimeDataStore.get(provideStringKey(key))
            .onEach { runtimeStatuses ->
                val mergedStatuses = mergeStatuses(
                    cachedStatuses = cachedStatuses,
                    runtimeStatuses = runtimeStatuses,
                )

                send(mergedStatuses)
            }
            .launchIn(scope = this)
    }

    override suspend fun getSyncOrNull(key: UserWalletId): Set<NetworkStatus>? {
        return runtimeDataStore.getSyncOrNull(provideStringKey(key))
    }

    override suspend fun store(key: UserWalletId, value: NetworkStatus) {
        mutex.withLock {
            val newValues = getSyncOrNull(key)
                ?.addOrReplace(value) { it.network == value.network }
                ?: setOf(value)

            runtimeDataStore.store(provideStringKey(key), newValues)
            storeNetworkStatusInPersistence(key, value)
        }
    }

    override suspend fun storeAll(key: UserWalletId, values: Collection<NetworkStatus>) {
        mutex.withLock {
            val currentValues = getSyncOrNull(key) ?: emptySet()
            val updatedValues = currentValues.toMutableSet()

            values.forEach { newValue ->
                val isReplaced = updatedValues.replaceBy(newValue) {
                    it.network == newValue.network
                }

                if (!isReplaced) {
                    updatedValues.add(newValue)
                }
            }

            runtimeDataStore.store(provideStringKey(key), updatedValues)
        }
    }

    /**
     * Merge [cachedStatuses] with [runtimeStatuses]
     * The resulting set contains statuses from both sets.
     * If a status with the same network is in both sets, the status from [runtimeStatuses] is used.
     */
    private fun mergeStatuses(
        cachedStatuses: Set<NetworkStatus>,
        runtimeStatuses: Set<NetworkStatus>,
    ): Set<NetworkStatus> {
        return runtimeStatuses
            .map { runtime ->
                runtime.takeIf { it.value !is NetworkStatus.Unreachable }
                    ?: getCachedStatusIfPossible(cachedStatuses = cachedStatuses, runtime = runtime)
            }
            .toSet()
    }

    private fun getCachedStatusIfPossible(cachedStatuses: Set<NetworkStatus>, runtime: NetworkStatus): NetworkStatus {
        val cached = cachedStatuses.firstOrNull { it.network == runtime.network } ?: return runtime

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
            runtime
        }
    }

    private suspend fun storeNetworkStatusInPersistence(userWalletId: UserWalletId, networkStatus: NetworkStatus) {
        val network = networkStatus.network

        // Converter will return null if the network status is not supported
        val newStatus = NetworkStatusDataModelConverter.convert(value = networkStatus) ?: return

        persistenceDataStore.updateData { storedStatuses ->
            val userWalletStatuses = storedStatuses[userWalletId.stringValue].orEmpty()
            val updatedStatuses = userWalletStatuses.addOrReplace(item = newStatus) { it.networkId == network.id }

            storedStatuses.toMutableMap().apply {
                this[userWalletId.stringValue] = updatedStatuses
            }
        }
    }

    private fun provideStringKey(key: UserWalletId): String {
        return "network_statuses_${key.stringValue}"
    }
}
