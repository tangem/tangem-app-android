package com.tangem.data.networks.store

import android.content.Context
import androidx.datastore.core.DataStore
import com.tangem.data.networks.converters.NetworkStatusDataModelConverter
import com.tangem.data.networks.converters.SimpleNetworkStatusConverter
import com.tangem.data.networks.models.SimpleNetworkStatus
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.network.entity.NetworkStatusDM
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

internal typealias WalletIdWithSimpleStatus = Map<String, Set<SimpleNetworkStatus>>
internal typealias WalletIdWithStatusDM = Map<String, Set<NetworkStatusDM>>

/**
 * Default implementation of [NetworksStatusesStore]
 *
 * @param context                 context
 * @property runtimeStore         runtime store
 * @property persistenceDataStore persistence store
 * @param dispatchers             dispatchers
 */
internal class DefaultNetworksStatusesStore(
    context: Context,
    private val runtimeStore: RuntimeSharedStore<WalletIdWithSimpleStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithStatusDM>,
    private val scope: AppCoroutineScope,
) : NetworksStatusesStore {

    init {
        scope.launch {
            try {
                val oldFile = File(context.filesDir, "datastore/networks_statuses")

                if (oldFile.exists()) {
                    oldFile.delete()
                }
            } catch (e: Exception) {
                TangemLogger.e("Error while deleting old networks statuses datastore file", e)
            }

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
            .adaptiveThrottle()
            .conflate()
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
            TangemLogger.d("Nothing to update: networks are empty")
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

    override suspend fun clear(userWalletId: UserWalletId, networks: Set<Network>) {
        persistenceDataStore.updateData { storedStatuses ->
            storedStatuses.toMutableMap().apply {
                val updatedValues = this[userWalletId.stringValue].orEmpty().filterNot {
                    networks.any { network ->
                        it.networkId.value == network.rawId && it.derivationPath.value == network.derivationPath.value
                    }
                }

                this[userWalletId.stringValue] = updatedValues.toSet()
            }
        }
    }

    override suspend fun contains(userWalletId: UserWalletId): Boolean {
        return runtimeStore.getSyncOrDefault(emptyMap()).containsKey(userWalletId.stringValue)
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

@Suppress("MagicNumber")
internal fun <T> Flow<Set<T>>.adaptiveThrottle(): Flow<Set<T>> = channelFlow {
    var accumulator: Set<T>? = null
    var lastEmitTime = 0L

    // params that control maximum emissions that can be throttled
    var densityLevel = 0
    val maxDensity = 10

    // params that control maximum delay and growth of delay between emissions
    var lastDelay = 0L
    val maxDelay = 1500L
    val growthFactor = 250L

    fun resetThrottling() {
        lastDelay = 0L
        densityLevel = 0
    }

    this@adaptiveThrottle.collectLatest { newSet ->
        val previousSet: Collection<T>? = accumulator
        accumulator = newSet

        when {
            // first value, just emit
            previousSet == null -> resetThrottling()
            // changed size, just emit
            previousSet.size != newSet.size -> resetThrottling()

            // apply adaptive throttling
            else -> {
                val networksCount = newSet.size
                // more networks - more throttling
                val cooldownThreshold = when {
                    networksCount in 10..25 -> 300L
                    networksCount > 25 -> 500L
                    // 0..9 networks
                    else -> 100L
                }

                val now = System.currentTimeMillis()
                val timeSinceLastEmit = now - lastEmitTime
                if (timeSinceLastEmit < cooldownThreshold && densityLevel < maxDensity) {
                    lastDelay = (lastDelay + growthFactor).coerceAtMost(maximumValue = maxDelay)
                    densityLevel += 1
                    delay(lastDelay)
                } else {
                    resetThrottling()
                }
            }
        }

        lastEmitTime = System.currentTimeMillis()
        channel.send(newSet)
    }
}