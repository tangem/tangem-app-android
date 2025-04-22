package com.tangem.data.networks.store

import androidx.datastore.core.DataStore
import com.tangem.data.networks.converters.SimpleNetworkStatusConverter
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.converter.NetworkStatusDataModelConverter
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias WalletIdWithSimpleStatus = Map<String, Set<SimpleNetworkStatus>>
internal typealias WalletIdWithStatusDM = Map<String, Set<NetworkStatusDM>>

/**
 * Default implementation of [NetworksStatusesStoreV2]
 *
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store
 * @param dispatchers             dispatchers
 */
internal class DefaultNetworksStatusesStoreV2(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithSimpleStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithStatusDM>,
    dispatchers: CoroutineDispatcherProvider,
) : NetworksStatusesStoreV2 {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedStatuses = persistenceDataStore.data.firstOrNull() ?: return@launch

            runtimeStore.store(
                value = cachedStatuses.mapValues { (_, statuses) ->
                    SimpleNetworkStatusConverter.convertSet(statuses)
                },
            )
        }
    }

    override fun get(userWalletId: UserWalletId): Flow<Set<SimpleNetworkStatus>> {
        return runtimeStore.get().mapNotNull { it[userWalletId.stringValue] }
    }

    override suspend fun refresh(userWalletId: UserWalletId, network: Network) {
        refresh(userWalletId = userWalletId, networks = setOf(network))
    }

    override suspend fun refresh(userWalletId: UserWalletId, networks: Set<Network>) {
        if (networks.isEmpty()) {
            Timber.d("Nothing to refresh: networks is empty")
            return
        }

        updateInRuntime(userWalletId = userWalletId, networks = networks) { status ->
            status.copy(value = status.value.copySealed(source = StatusSource.CACHE))
        }
    }

    override suspend fun storeActual(userWalletId: UserWalletId, value: NetworkStatus) {
        if (value.value.source != StatusSource.ACTUAL) {
            error("Method storeActual can be called only with StatusSource.ACTUAL")
        }

        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = value) }
            launch { storeInPersistence(userWalletId = userWalletId, status = value) }
        }
    }

    override suspend fun storeError(userWalletId: UserWalletId, network: Network) {
        updateInRuntime(
            userWalletId = userWalletId,
            networks = setOf(network),
            ifNotFound = ::createUnreachableStatus,
            update = { status ->
                status.copy(value = status.value.copySealed(source = StatusSource.ONLY_CACHE))
            },
        )
    }

    private suspend fun updateInRuntime(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        ifNotFound: (SimpleNetworkStatus.Id) -> SimpleNetworkStatus? = { null },
        update: (SimpleNetworkStatus) -> SimpleNetworkStatus,
    ) {
        val simpleStatusIds = networks.map(SimpleNetworkStatus::Id)

        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val storedStatuses = this[userWalletId.stringValue].orEmpty()

                val statuses = simpleStatusIds.mapNotNullTo(hashSetOf()) { simpleStatusId ->
                    val status = storedStatuses.firstOrNull { it.id == simpleStatusId }
                        ?: ifNotFound(simpleStatusId)
                        ?: return@mapNotNullTo null

                    update(status)
                }

                val updatedStatuses = storedStatuses.addOrReplace(statuses) { old, new -> old.id == new.id }

                put(key = userWalletId.stringValue, value = updatedStatuses)
            }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: NetworkStatus) {
        val simpleStatusId = SimpleNetworkStatus.Id(network = status.network)

        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val simpleStatus = SimpleNetworkStatus(id = simpleStatusId, value = status.value)

                val updatedStatuses = this[userWalletId.stringValue].orEmpty()
                    .addOrReplace(simpleStatus) { it.id == simpleStatusId }

                put(key = userWalletId.stringValue, value = updatedStatuses)
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, status: NetworkStatus) {
        // Converter will return null if the network status is not supported
        val statusDM = NetworkStatusDataModelConverter.convert(value = status) ?: return

        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                val updatedValues = this[userWalletId.stringValue].orEmpty().addOrReplace(statusDM) {
                    it.networkId == statusDM.networkId && it.derivationPath == statusDM.derivationPath
                }

                this[userWalletId.stringValue] = updatedValues
            }
        }
    }

    private fun createUnreachableStatus(id: SimpleNetworkStatus.Id): SimpleNetworkStatus {
        return SimpleNetworkStatus(id = id, value = NetworkStatus.Unreachable(address = null))
    }
}