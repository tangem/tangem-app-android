package com.tangem.datasource.local.network

import androidx.datastore.core.DataStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.entity.NetworkStatusesDM
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
    private val persistneceDataStore: DataStore<NetworkStatusesDM>,
) : NetworksStatusesStore {

    private val mutex = Mutex()

    override fun get(key: UserWalletId): Flow<Set<NetworkStatus>> {
        return runtimeDataStore.get(provideStringKey(key))
    }

    override fun get(key: UserWalletId, networks: Set<Network>): Flow<Set<NetworkStatus>> = channelFlow {
        val cachedStatuses = persistneceDataStore.data.firstOrNull()
            ?.get(key.stringValue)
            ?.mapNotNullTo(mutableSetOf()) { status ->
                val network = networks
                    .firstOrNull { it.id == status.networkId }
                    ?: return@mapNotNullTo null

                status.toDomainModel(network)
            }
            .orEmpty()

        if (cachedStatuses.isNotEmpty()) {
            send(cachedStatuses)
        }

        runtimeDataStore.get(provideStringKey(key))
            .onEach { runtimeStatuses ->
                if (cachedStatuses.isEmpty()) {
                    send(runtimeStatuses)
                } else {
                    val mergedStatuses = cachedStatuses.toMutableList()

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

    private suspend fun storeNetworkStatusInPersistence(userWalletId: UserWalletId, networkStatus: NetworkStatus) {
        val status = networkStatus.value
        val network = networkStatus.network

        if (status !is NetworkStatus.Verified) return

        persistneceDataStore.updateData { storedStatuses ->
            val userWalletStatuses = storedStatuses[userWalletId.stringValue] ?: emptySet()
            val updatedStatuses = userWalletStatuses.addOrReplace(status.toDataModel(network)) {
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