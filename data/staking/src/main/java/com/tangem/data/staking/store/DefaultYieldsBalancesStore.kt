package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
import com.tangem.domain.models.StatusSource
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance
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

internal typealias WalletIdWithWrappers = Map<String, Set<YieldBalanceWrapperDTO>>
internal typealias WalletIdWithBalances = Map<UserWalletId, Set<YieldBalance>>

/**
 * Default implementation of [YieldsBalancesStore]
 *
 * @property runtimeStore     runtime store
 * @property persistenceStore persistence store
 * @param dispatchers
 *
* [REDACTED_AUTHOR]
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

                    key to value
                }
                    .toMap(),
            )
        }
    }

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldBalance>> {
        return runtimeStore.get().mapNotNull { it[userWalletId] }
    }

    override suspend fun refresh(userWalletId: UserWalletId, stakingId: StakingID) {
        updateBalanceInRuntime(userWalletId, stakingId) {
            it.copySealed(source = StatusSource.CACHE)
        }
    }

    override suspend fun refresh(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val storedBalances = this[userWalletId].orEmpty()

                val balances = stakingIds.mapTo(hashSetOf()) { stakingId ->
                    val balance = storedBalances.firstOrNull {
                        it.integrationId == stakingId.integrationId &&
                            it.address == stakingId.address
                    }
                        ?: createDefaultBalance(id = stakingId)

                    balance.copySealed(source = StatusSource.CACHE)
                }

                val updatedBalances = storedBalances.addOrReplace(balances) { old, new ->
                    old.integrationId == new.integrationId && old.address == new.address
                }

                put(key = userWalletId, value = updatedBalances)
            }
        }
    }

    override suspend fun storeActual(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, values = values) }
            launch { storeInPersistence(userWalletId = userWalletId, values = values) }
        }
    }

    override suspend fun storeError(userWalletId: UserWalletId, stakingId: StakingID) {
        updateBalanceInRuntime(userWalletId, stakingId) {
            it.copySealed(source = StatusSource.ONLY_CACHE)
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        val newBalances = YieldBalanceConverter(isCached = false).convertSet(input = values)

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

    private suspend fun storeInPersistence(userWalletId: UserWalletId, values: Set<YieldBalanceWrapperDTO>) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = current[userWalletId.stringValue]
                    ?.addOrReplace(items = values) { old, new ->
                        old.integrationId == new.integrationId && old.addresses.address == new.addresses.address
                    }
                    ?: values
            }
        }
    }

    private suspend fun updateBalanceInRuntime(
        userWalletId: UserWalletId,
        stakingID: StakingID,
        update: (YieldBalance) -> YieldBalance,
    ) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                val balance = this[userWalletId].orEmpty()
                    .firstOrNull {
                        it.integrationId == stakingID.integrationId &&
                            it.address == stakingID.address
                    }
                    ?: createDefaultBalance(id = stakingID)

                val updatedBalances = this[userWalletId].orEmpty()
                    .addOrReplace(item = update(balance)) {
                        it.integrationId == balance.integrationId &&
                            it.address == balance.address
                    }

                put(key = userWalletId, value = updatedBalances)
            }
        }
    }

    private fun createDefaultBalance(id: StakingID): YieldBalance {
        return YieldBalance.Error(integrationId = id.integrationId, address = id.address)
    }
}
