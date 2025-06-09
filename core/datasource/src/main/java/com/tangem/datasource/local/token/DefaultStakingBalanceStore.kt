package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.StakingBalanceStore.StakingID
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

    override fun get(userWalletId: UserWalletId, stakingIds: List<StakingID>): Flow<Set<YieldBalance>> = channelFlow {
        val cachedBalances = persistenceStore.data
            .map {
                val wrappers = it[userWalletId.stringValue].orEmpty()
                    .filter { wrapper ->
                        stakingIds.any { id ->
                            id.address == wrapper.addresses.address && id.integrationId == wrapper.integrationId
                        }
                    }

                YieldBalanceConverter(isCached = true).convertSet(input = wrappers)
            }
            .firstOrNull()
            .orEmpty()

        if (cachedBalances.isNotEmpty()) {
            send(cachedBalances)
        }

        runtimeStore.get()
            .map {
                it[userWalletId].orEmpty().filter { balance ->
                    stakingIds.any { id ->
                        id.address == balance.address && id.integrationId == balance.integrationId
                    }
                }
                    .toSet()
            }
            .onEach {
                val mergedBalances = mergeYieldBalances(
                    stakingIds = stakingIds,
                    cachedBalances = cachedBalances,
                    runtimeBalances = it,
                )

                send(mergedBalances)
            }
            .launchIn(scope = this)
    }

    override fun get(userWalletId: UserWalletId, stakingID: StakingID): Flow<YieldBalance?> {
        return get(userWalletId = userWalletId, stakingIds = listOf(stakingID)).map { balances ->
            balances.getBalance(stakingID = stakingID)
        }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>? {
        val runtimeBalances = runtimeStore.getSyncOrNull()?.getValue(userWalletId).orEmpty()
        val cachedBalances = persistenceStore.data.firstOrNull()?.get(userWalletId.stringValue).orEmpty()

        if (runtimeBalances.isEmpty() && cachedBalances.isEmpty()) return null

        return cachedBalances.mapTo(hashSetOf()) {
            val cached = YieldBalanceConverter(source = StatusSource.ONLY_CACHE).convert(value = it)
            val runtime = runtimeBalances.getBalance(address = cached.address, integrationId = cached.integrationId)

            if (runtime == null || runtime is YieldBalance.Error) cached else runtime
        }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingIds: List<StakingID>): Set<YieldBalance>? {
        val runtime = runtimeStore.getSyncOrNull()?.getValue(userWalletId)
        val cached = persistenceStore.data.firstOrNull()?.get(userWalletId.stringValue)

        if (runtime.isNullOrEmpty() && cached.isNullOrEmpty()) return null

        return mergeYieldBalances(
            cachedBalances = YieldBalanceConverter(source = StatusSource.ONLY_CACHE)
                .convertSet(input = cached.orEmpty()),
            runtimeBalances = runtime.orEmpty(),
            stakingIds = stakingIds,
        )
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingID: StakingID): YieldBalance? {
        val balances = getSyncOrNull(userWalletId = userWalletId, stakingIds = listOf(stakingID)) ?: return null

        return balances.getBalance(stakingID = stakingID)
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

    override suspend fun refresh(userWalletId: UserWalletId, stakingIds: List<StakingID>) {
        updateRuntimeStore(userWalletId = userWalletId) { saved ->
            saved.mapTo(hashSetOf()) {
                val yieldBalance = it.takeIf { balance ->
                    stakingIds.any { id -> balance.integrationId == id.integrationId && balance.address == id.address }
                }

                yieldBalance?.copySealed(source = StatusSource.CACHE) ?: it
            }
        }
    }

    override suspend fun store(userWalletId: UserWalletId, stakingID: StakingID, item: YieldBalanceWrapperDTO) {
        coroutineScope {
            launch {
                storeInRuntimeStore(
                    userWalletId = userWalletId,
                    integrationId = stakingID.integrationId,
                    address = stakingID.address,
                    item = item,
                )

                storeInPersistenceStore(
                    userWalletId = userWalletId,
                    integrationId = stakingID.integrationId,
                    address = stakingID.address,
                    item = item,
                )
            }
        }
    }

    override suspend fun storeSingleYieldBalance(userWalletId: UserWalletId, item: YieldBalance) {
        runtimeStore.update(default = emptyMap()) { saved ->
            saved.toMutableMap().apply {
                this[userWalletId] = saved[userWalletId]
                    ?.addOrReplace(item) { it.integrationId == item.integrationId && it.address == item.address }
                    ?: setOf(item)
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
        stakingIds: List<StakingID>,
    ): Set<YieldBalance> {
        return stakingIds.mapTo(hashSetOf()) { id ->
            val runtime = runtimeBalances.getBalance(stakingID = id)

            if (runtime == null || runtime is YieldBalance.Error) {
                getCachedBalanceIfPossible(cachedBalances = cachedBalances, stakingID = id)
            } else {
                runtime
            }
        }
    }

    private fun getCachedBalanceIfPossible(cachedBalances: Set<YieldBalance>, stakingID: StakingID): YieldBalance {
        val cached = cachedBalances.getBalance(stakingID)
            ?: return YieldBalance.Error(integrationId = stakingID.address, address = stakingID.integrationId)

        val updatedCached = when (cached) {
            is YieldBalance.Data -> cached.copy(source = StatusSource.ONLY_CACHE)
            is YieldBalance.Empty -> cached.copy(source = StatusSource.ONLY_CACHE)
            is YieldBalance.Error,
            is YieldBalance.Unsupported,
            -> null
        }

        return updatedCached ?: YieldBalance.Error(integrationId = stakingID.address, address = stakingID.integrationId)
    }

    private fun Set<YieldBalance>.getBalance(stakingID: StakingID): YieldBalance? {
        return getBalance(address = stakingID.address, integrationId = stakingID.integrationId)
    }

    private fun Set<YieldBalance>.getBalance(address: String?, integrationId: String?): YieldBalance? {
        return firstOrNull { yieldBalance ->
            val isCorrectAddress = address != null && address == yieldBalance.address
            val isCorrectIntegration = integrationId != null && yieldBalance.integrationId == integrationId

            isCorrectIntegration && isCorrectAddress
        }
    }
}