package com.tangem.data.networks.store

import androidx.datastore.core.DataStore
import com.tangem.data.networks.converters.NetworkStatusDataModelConverter
import com.tangem.data.networks.converters.SimpleNetworkStatusConverter
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
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
 * Default implementation of [NetworksStatusesStore]
 *
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store
 * @param dispatchers             dispatchers
 */
internal class DefaultNetworksStatusesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithSimpleStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithStatusDM>,
    dispatchers: CoroutineDispatcherProvider,
) : NetworksStatusesStore {

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

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, network: Network): SimpleNetworkStatus? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)?.firstOrNull { it.id == network.id }
    }

    override suspend fun updateStatusSource(
        userWalletId: UserWalletId,
        network: Network,
        source: StatusSource,
        ifNotFound: (Network.ID) -> SimpleNetworkStatus?,
    ) {
        updateStatusSource(
            userWalletId = userWalletId,
            networks = setOf(network),
            source = source,
            ifNotFound = ifNotFound,
        )
    }

    override suspend fun updateStatusSource(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        source: StatusSource,
        ifNotFound: (Network.ID) -> SimpleNetworkStatus?,
    ) {
        if (networks.isEmpty()) {
            Timber.d("Nothing to update: networks are empty")
            return
        }

        updateInRuntime(userWalletId = userWalletId, networks = networks, ifNotFound = ifNotFound) { status ->
            status.copy(value = status.value.copySealed(source = source))
        }
    }

    override suspend fun store(userWalletId: UserWalletId, status: NetworkStatus) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, status = status) }
            launch { storeInPersistence(userWalletId = userWalletId, status = status) }
        }
    }

    private suspend fun updateInRuntime(
        userWalletId: UserWalletId,
        networks: Set<Network>,
        ifNotFound: (Network.ID) -> SimpleNetworkStatus? = { null },
        update: (SimpleNetworkStatus) -> SimpleNetworkStatus,
    ) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val storedStatuses = this[userWalletId.stringValue].orEmpty()

                val statuses = networks.mapNotNullTo(hashSetOf()) { network ->
                    val status = storedStatuses.firstOrNull { it.id == network.id }
                        ?: ifNotFound(network.id)
                        ?: return@mapNotNullTo null

                    update(status)
                }

                val updatedStatuses = storedStatuses.addOrReplace(statuses) { old, new -> old.id == new.id }

                put(key = userWalletId.stringValue, value = updatedStatuses)
            }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, status: NetworkStatus) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val simpleStatus = SimpleNetworkStatus(status = status)

                val updatedStatuses = this[userWalletId.stringValue].orEmpty()
                    .addOrReplace(simpleStatus) { it.id == simpleStatus.id }

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
}