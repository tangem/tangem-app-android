package com.tangem.datasource.local.network

import androidx.datastore.core.DataStore
import com.tangem.datasource.local.datastore.RuntimeDataStore
import com.tangem.datasource.local.network.converter.NetworkDerivationPathConverter
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
            ?.mapNotNullTo(mutableSetOf()) { cached ->
                val network = networks.firstOrNull {
                    it.id == cached.networkId &&
                        it.derivationPath == NetworkDerivationPathConverter.convert(cached.derivationPath)
                }
                    ?: return@mapNotNullTo null

                NetworkStatusConverter(network = network, isCached = true).convert(value = cached)
            }
            .orEmpty()

        if (cachedStatuses.isNotEmpty()) {
            send(cachedStatuses)
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
        mutex.withLock {
            coroutineScope {
                launch { storeInRuntimeStore(key = key, statuses = values) }
                launch { storeInPersistenceStore(userWalletId = key, statuses = values) }
            }
        }
    }

    override suspend fun refresh(key: UserWalletId, networks: Set<Network>) {
        mutex.withLock {
            val currentStatuses = getSyncOrNull(key).orEmpty()

            storeInRuntimeStore(
                key = key,
                statuses = networks.mapNotNullTo(hashSetOf()) { network ->
                    val status = currentStatuses.firstOrNull {
                        it.network.id == network.id && it.network.derivationPath == network.derivationPath
                    } ?: return@mapNotNullTo null

                    status.copy(
                        value = status.value.copySealed(source = StatusSource.CACHE),
                    )
                },
            )
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

            if (runtimeStatus == null) {
                getCachedStatusIfPossible(
                    cachedStatuses = cachedStatuses,
                    network = network,
                    source = StatusSource.CACHE,
                )
            } else if (runtimeStatus.value is NetworkStatus.Unreachable) {
                getCachedStatusIfPossible(
                    cachedStatuses = cachedStatuses,
                    network = network,
                    source = StatusSource.ONLY_CACHE,
                )
                    ?: runtimeStatus
            } else {
                runtimeStatus
            }
        }
    }

    private fun getCachedStatusIfPossible(
        cachedStatuses: Set<NetworkStatus>,
        network: Network,
        source: StatusSource,
    ): NetworkStatus? {
        val cached = cachedStatuses.firstOrNull { it.network == network } ?: return null

        val updatedCachedStatus = when (val status = cached.value) {
            is NetworkStatus.NoAccount -> status.copy(source = source)
            is NetworkStatus.Verified -> status.copy(source = source)
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

    private suspend fun storeInRuntimeStore(key: UserWalletId, statuses: Set<NetworkStatus>) {
        val updatedValues = getSyncOrNull(key).orEmpty()
            .addOrReplace(items = statuses) { prev, new -> prev.network == new.network }

        runtimeDataStore.store(key = provideStringKey(key), value = updatedValues)
    }

    private suspend fun storeInPersistenceStore(userWalletId: UserWalletId, statuses: Set<NetworkStatus>) {
        // Converter will return null if the network status is not supported
        val newStatuses = NetworkStatusDataModelConverter.convertSet(input = statuses).filterNotNull().toSet()

        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                val updatedValues = this[userWalletId.stringValue].orEmpty()
                    .addOrReplace(newStatuses) { prev, new ->
                        prev.networkId == new.networkId && prev.derivationPath == new.derivationPath
                    }

                this[userWalletId.stringValue] = updatedValues
            }
        }
    }

    private fun provideStringKey(key: UserWalletId): String {
        return "network_statuses_${key.stringValue}"
    }
}