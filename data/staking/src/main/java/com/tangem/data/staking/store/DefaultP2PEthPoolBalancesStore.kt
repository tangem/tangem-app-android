package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.tangem.data.staking.converters.ethpool.P2PEthPoolStakingBalanceConverter
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal typealias WalletIdWithP2PStakingBalances = Map<UserWalletId, Set<StakingBalance>>
internal typealias WalletIdWithP2PEthPoolResponses = Map<String, Set<P2PEthPoolAccountResponse>>

/**
 * Default implementation of [P2PEthPoolBalancesStore]
 *
 * Stores P2PEthPool staking balances.
 *
 * @property runtimeStore runtime store
 * @property persistenceStore persistence store
 * @param dispatchers coroutine dispatchers
 */
internal class DefaultP2PEthPoolBalancesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithP2PStakingBalances>,
    private val persistenceStore: DataStore<WalletIdWithP2PEthPoolResponses>,
    dispatchers: CoroutineDispatcherProvider,
) : P2PEthPoolBalancesStore {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedData = persistenceStore.data.firstOrNull() ?: return@launch

            runtimeStore.store(
                value = cachedData.map { (stringWalletId, responses) ->
                    val key = UserWalletId(stringWalletId)
                    val value = P2PEthPoolStakingBalanceConverter.convertAll(
                        responses = responses,
                        source = StatusSource.CACHE,
                    )

                    key to value
                }.toMap(),
            )
        }
    }

    override fun get(userWalletId: UserWalletId): Flow<Set<StakingBalance>> {
        return runtimeStore.get().map { it[userWalletId].orEmpty() }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId, stakingId: StakingID): StakingBalance? {
        return runtimeStore.getSyncOrNull()
            ?.get(userWalletId)
            ?.firstOrNull { it.stakingId == stakingId }
    }

    override suspend fun getAllSyncOrNull(userWalletId: UserWalletId): Set<StakingBalance>? {
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

    override suspend fun storeActual(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>) {
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, values = values) }
            launch { storeInPersistence(userWalletId = userWalletId, values = values) }
        }
    }

    override suspend fun storeEmpty(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        updateInRuntime(
            userWalletId = userWalletId,
            stakingIds = stakingIds,
            ifNotFound = ::createEmptyStakingBalance,
            update = { it.copySealed(source = StatusSource.ACTUAL) },
        )
    }

    override suspend fun storeError(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        updateInRuntime(
            userWalletId = userWalletId,
            stakingIds = stakingIds,
            ifNotFound = ::createErrorStakingBalance,
            update = { it.copySealed(source = StatusSource.ONLY_CACHE) },
        )
    }

    override suspend fun clear(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        coroutineScope {
            launch { clearInRuntime(userWalletId = userWalletId, stakingIds = stakingIds) }
            launch { clearInPersistence(userWalletId = userWalletId, stakingIds = stakingIds) }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>) {
        val newBalances = P2PEthPoolStakingBalanceConverter.convertAll(
            responses = values,
            source = StatusSource.ACTUAL,
        )

        runtimeStore.update(default = emptyMap()) { saved ->
            saved.toMutableMap().apply {
                this[userWalletId] = saved[userWalletId]
                    ?.addOrReplace(newBalances) { old, new -> old.stakingId == new.stakingId }
                    ?: newBalances
            }
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = this[userWalletId.stringValue]
                    ?.addOrReplace(values) { old, new ->
                        old.delegatorAddress == new.delegatorAddress && old.vaultAddress == new.vaultAddress
                    }
                    ?: values
            }
        }
    }

    private suspend fun clearInRuntime(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored.toMutableMap().apply {
                this[userWalletId] = this[userWalletId].orEmpty()
                    .filterNot { it.stakingId in stakingIds }
                    .toSet()
            }
        }
    }

    private suspend fun clearInPersistence(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        val addressesToClear = stakingIds.map { it.address }.toSet()

        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = this[userWalletId.stringValue].orEmpty()
                    .filterNot { response ->
                        response.delegatorAddress in addressesToClear
                    }
                    .toSet()
            }
        }
    }

    private suspend fun updateInRuntime(
        userWalletId: UserWalletId,
        stakingIds: Set<StakingID>,
        ifNotFound: (StakingID) -> StakingBalance? = { null },
        update: (StakingBalance) -> StakingBalance,
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

    private fun createEmptyStakingBalance(id: StakingID): StakingBalance =
        StakingBalance.Empty(stakingId = id, source = StatusSource.ACTUAL)

    private fun createErrorStakingBalance(id: StakingID): StakingBalance = StakingBalance.Error(stakingId = id)
}