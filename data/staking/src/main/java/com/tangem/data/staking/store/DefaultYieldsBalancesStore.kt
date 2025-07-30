package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal typealias WalletIdWithWrappers = Map<String, Set<YieldBalanceWrapperDTO>>
internal typealias WalletIdWithBalances = Map<UserWalletId, Set<YieldBalance>>

/**
 * Default implementation of [YieldsBalancesStore]
 *
 * @property runtimeStore     runtime store
 * @property persistenceStore persistence store
 * @param dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultYieldsBalancesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithBalances>,
    private val persistenceStore: DataStore<WalletIdWithWrappers>,
    dispatchers: CoroutineDispatcherProvider,
) : YieldsBalancesStore {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedStatuses = persistenceStore.data.firstOrNull() ?: return@launch

            runtimeStore.store(
                value = cachedStatuses.map { (stringWalletId, wrappers) ->
                    val key = UserWalletId(stringWalletId)
                    val value = YieldBalanceConverter(isCached = true).convertSet(input = wrappers)
                        .filterNotNull()
                        .toSet()

                    key to value
                }
                    .toMap(),
            )
        }
    }

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>> {
        return runtimeStore.get().map { it[userWalletId].orEmpty() }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): YieldBalance? {
        return runtimeStore.getSyncOrNull()
            ?.get(userWalletId)
            ?.firstOrNull { it.stakingId == stakingId }
    }

    override suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<YieldBalance>? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId)
    }

    override suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID) {
        refresh(userWalletId = userWalletId, stakingIds = setOf(stakingId))
    }

    override suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        updateInRuntime(userWalletId = userWalletId, stakingIds = stakingIds) {
            it.copySealed(source = StatusSource.CACHE)
        }
    }

    override suspend fun storeActual(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, values = values) }
            launch { storeInPersistence(userWalletId = userWalletId, values = values) }
        }
    }

    override suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        updateInRuntime(
            userWalletId = userWalletId,
            stakingIds = stakingIds,
            ifNotFound = ::createErrorYieldBalance,
            update = { it.copySealed(source = StatusSource.ONLY_CACHE) },
        )
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        val newBalances = YieldBalanceConverter(isCached = false).convertSet(input = values)
            .filterNotNull()
            .toSet()

        runtimeStore.update(default = emptyMap()) { saved ->
            saved.toMutableMap().apply {
                this[userWalletId] = saved[userWalletId]
                    ?.addOrReplace(newBalances) { old, new -> old.stakingId == new.stakingId }
                    ?: newBalances
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = this[userWalletId.stringValue]
                    ?.addOrReplace(items = values) { old, new ->
                        val oldId = old.getStakingId()
                        val newId = new.getStakingId()

                        if (oldId == null || newId == null) return@addOrReplace false

                        oldId == newId
                    }
                    ?: values
            }
        }
    }

    private suspend fun updateInRuntime(
        userWalletId: UserWalletId,
        stakingIds: Set<StakingID>,
        ifNotFound: (StakingID) -> YieldBalance? = { null },
        update: (YieldBalance) -> YieldBalance,
    ) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val portfolioBalances = stored[userWalletId].orEmpty()

                val balances = stakingIds.mapNotNullTo(hashSetOf()) { stakingId ->
                    val balance = portfolioBalances
                        .firstOrNull { it.stakingId == stakingId }
                        ?: ifNotFound(stakingId)
                        ?: return@mapNotNullTo null

                    update(balance)
                }

                val updatedBalances = portfolioBalances.addOrReplace(items = balances) { old, new ->
                    old.stakingId == new.stakingId
                }

                put(key = userWalletId, value = updatedBalances)
            }
        }
    }

    private fun createErrorYieldBalance(id: StakingID): YieldBalance = YieldBalance.Error(stakingId = id)

    private fun YieldBalanceWrapperDTO.getStakingId(): StakingID? {
        val integrationId = integrationId
        val address = addresses.address

        if (integrationId == null || address.isBlank()) return null

        return StakingID(integrationId = integrationId, address = address)
    }
}