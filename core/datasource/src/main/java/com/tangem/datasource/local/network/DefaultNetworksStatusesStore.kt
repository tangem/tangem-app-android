package com.tangem.datasource.local.network

import androidx.datastore.core.DataStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.entity.NetworkStatusesByWalletId
import com.tangem.datasource.local.network.utils.toDataModel
import com.tangem.datasource.local.network.utils.toDomainModel
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.extensions.replaceBy
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

                status.toDomainModel(network = network, isCached = true)
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
        val runtimeMap = runtimeStatuses.associateBy { it.network }
        val mergedCached = cachedStatuses.filter { it.network !in runtimeMap.keys }

        return mergedCached.toSet() + runtimeStatuses
    }

    private suspend fun storeNetworkStatusInPersistence(userWalletId: UserWalletId, networkStatus: NetworkStatus) {
        val status = networkStatus.value
        val network = networkStatus.network

        if (status !is NetworkStatus.Verified) return

        persistenceDataStore.updateData { storedStatuses ->
            val userWalletStatuses = storedStatuses[userWalletId.stringValue] ?: emptySet()
            val updatedStatuses = userWalletStatuses.addOrReplace(item = status.toDataModel(network)) {
                it.networkId == network.id
            }

            storedStatuses.toMutableMap().apply {
                this[userWalletId.stringValue] = updatedStatuses
            }
        }
    }

    private fun provideStringKey(key: UserWalletId): String {
        return "network_statuses_${key.stringValue}"
    }
}