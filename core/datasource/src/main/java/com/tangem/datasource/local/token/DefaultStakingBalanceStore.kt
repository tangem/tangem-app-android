package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias YieldBalanceWrappersDTO = Map<String, Set<YieldBalanceWrapperDTO>>
internal typealias YieldBalanceListByWalletId = Map<UserWalletId, Set<YieldBalance>>

/**
 * Default implementation of [StakingBalanceStore]
 *
 * @property persistenceStore persistence store
 * @property runtimeStore     runtime store
 */
internal class DefaultStakingBalanceStore(
    private val persistenceStore: DataStore<YieldBalanceWrappersDTO>,
    private val runtimeStore: RuntimeSharedStore<YieldBalanceListByWalletId>,
) : StakingBalanceStore {

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>> = channelFlow {
        val cachedBalances = persistenceStore.data
            .map {
                val wrappers = it[userWalletId.stringValue].orEmpty()
                YieldBalanceConverter(isCached = true).convertSet(input = wrappers)
            }
            .firstOrNull()
            .orEmpty()

        if (cachedBalances.isNotEmpty()) {
            send(cachedBalances)
        }

        runtimeStore.get()
            .map { it[userWalletId].orEmpty() }
            .onEach {
                val mergedBalances = mergeYieldBalances(cachedBalances = cachedBalances, runtimeBalances = it)

                send(mergedBalances)
            }
            .launchIn(scope = this)
    }

    override fun get(userWalletId: UserWalletId, address: String, integrationId: String): Flow<YieldBalance?> {
        return get(userWalletId).map { balances ->
            balances.getBalance(address = address, integrationId = integrationId)
        }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>? {
        return runtimeStore.getSyncOrNull()?.getValue(userWalletId)
    }

    override suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): YieldBalance? {
        val balances = getSyncOrNull(userWalletId) ?: return null

        return balances.getBalance(address, integrationId)
    }

    override suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>) {
        coroutineScope {
            launch {
                val newBalances = YieldBalanceConverter(isCached = false).convertSet(input = items)

                runtimeStore.update(default = emptyMap()) { saved ->
                    saved.toMutableMap().apply {
                        this[userWalletId] = saved[userWalletId]
                            ?.addOrReplace(newBalances) { old, new ->
                                old.integrationId == new.integrationId && old.address == new.address
                            }
                            ?: newBalances
                    }
                }
            }
            launch { storeInPersistenceStore(userWalletId = userWalletId, items = items) }
        }
    }

    override suspend fun refresh(userWalletId: UserWalletId, addressWithIntegrationIdMap: Map<String, String>) {
        updateRuntimeStore(userWalletId = userWalletId) { saved ->
            saved.mapTo(hashSetOf()) { balance ->
                val refreshIntegrationId = addressWithIntegrationIdMap[balance.address]

                if (balance.integrationId == refreshIntegrationId) {
                    when (balance) {
                        is YieldBalance.Data -> balance.copy(source = StatusSource.CACHE)
                        is YieldBalance.Empty -> balance.copy(source = StatusSource.CACHE)
                        is YieldBalance.Error -> balance
                    }
                } else {
                    balance
                }
            }
        }
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        integrationId: String,
        address: String,
        item: YieldBalanceWrapperDTO,
    ) {
        coroutineScope {
            launch {
                storeInRuntimeStore(userWalletId, integrationId, address, item)
                storeInPersistenceStore(userWalletId, integrationId, address, item)
            }
        }
    }

    private suspend fun storeInRuntimeStore(
        userWalletId: UserWalletId,
        integrationId: String,
        address: String,
        item: YieldBalanceWrapperDTO,
    ) {
        val newBalance = YieldBalanceConverter(isCached = false).convert(value = item)

        runtimeStore.update(default = emptyMap()) { saved ->
            saved.toMutableMap().apply {
                this[userWalletId] = saved[userWalletId]
                    ?.addOrReplace(newBalance) { it.integrationId == integrationId && it.address == address }
                    ?: setOf(newBalance)
            }
        }
    }

    private suspend fun updateRuntimeStore(
        userWalletId: UserWalletId,
        function: (Set<YieldBalance>) -> Set<YieldBalance>,
    ) {
        runtimeStore.update(default = emptyMap()) { saved ->
            saved.toMutableMap().apply {
                this[userWalletId] = function(this[userWalletId].orEmpty())
            }
        }
    }

    private suspend fun storeInPersistenceStore(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = current[userWalletId.stringValue]
                    ?.addOrReplace(items = items) { old, new ->
                        old.integrationId == new.integrationId && old.addresses.address == new.addresses.address
                    }
                    ?: items
            }
        }
    }

    private suspend fun storeInPersistenceStore(
        userWalletId: UserWalletId,
        integrationId: String,
        address: String,
        item: YieldBalanceWrapperDTO,
    ) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = current[userWalletId.stringValue]
                    ?.addOrReplace(item) { it.integrationId == integrationId && it.addresses.address == address }
                    ?: setOf(item)
            }
        }
    }

    private fun mergeYieldBalances(
        cachedBalances: Set<YieldBalance>,
        runtimeBalances: Set<YieldBalance>,
    ): Set<YieldBalance> {
        if (runtimeBalances.isEmpty()) return cachedBalances

        return runtimeBalances
            .map { runtime ->
                runtime.takeIf { runtime !is YieldBalance.Error }
                    ?: getCachedBalanceIfPossible(cachedBalances, runtime)
            }
            .toSet()
    }

    private fun getCachedBalanceIfPossible(cachedBalances: Set<YieldBalance>, runtime: YieldBalance): YieldBalance {
        val cached = cachedBalances.getBalance(address = runtime.address, integrationId = runtime.integrationId)
            ?: return runtime

        val updatedCached = when (cached) {
            is YieldBalance.Data -> cached.copy(source = StatusSource.ONLY_CACHE)
            is YieldBalance.Empty -> cached.copy(source = StatusSource.ONLY_CACHE)
            is YieldBalance.Error -> null
        }

        return updatedCached ?: runtime
    }

    private fun Set<YieldBalance>.getBalance(address: String?, integrationId: String?): YieldBalance? {
        return firstOrNull { yieldBalance ->
            val isCorrectAddress = address != null && address == yieldBalance.address
            val isCorrectIntegration = integrationId != null && yieldBalance.integrationId == integrationId

            isCorrectIntegration && isCorrectAddress
        }
    }
}