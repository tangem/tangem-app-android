package com.tangem.data.staking.store

import androidx.datastore.core.DataStore
import com.tangem.data.staking.converters.ethpool.P2PEthPoolAccountConverter
import com.tangem.data.staking.converters.ethpool.P2PYieldBalanceConverter
import com.tangem.datasource.api.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

internal typealias WalletIdWithP2PBalances = Map<UserWalletId, Set<YieldBalance>>
internal typealias WalletIdWithP2PResponses = Map<String, Set<P2PEthPoolAccountResponse>>

/**
 * Default implementation of [P2PBalancesStore]
 *
 * Stores P2P ETH Pool staking balances with persistence support.
 *
 * @property runtimeStore runtime store
 * @property persistenceStore persistence store
 * @property vaultsProvider provider for vaults
 * @param dispatchers coroutine dispatchers
 */
internal class DefaultP2PBalancesStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithP2PBalances>,
    private val persistenceStore: DataStore<WalletIdWithP2PResponses>,
    private val vaultsProvider: suspend () -> List<P2PEthPoolVault>,
    dispatchers: CoroutineDispatcherProvider,
) : P2PBalancesStore {

    private val scope = CoroutineScope(context = SupervisorJob() + dispatchers.io)

    init {
        scope.launch {
            val cachedData = persistenceStore.data.firstOrNull() ?: return@launch
            val vaults = vaultsProvider()

            runtimeStore.store(
                value = cachedData.map { (stringWalletId, responses) ->
                    val key = UserWalletId(stringWalletId)
                    val value = responses.mapNotNull { response ->
                        val vault = vaults.firstOrNull { it.vaultAddress == response.vaultAddress }
                            ?: return@mapNotNull null
                        val account = P2PEthPoolAccountConverter.convert(response)
                        P2PYieldBalanceConverter.convert(
                            account = account,
                            vault = vault,
                            address = account.delegatorAddress,
                            source = StatusSource.CACHE,
                        )
                    }.toSet()

                    key to value
                }.toMap(),
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

    override suspend fun storeActual(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>) {
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

    override suspend fun clear(userWalletId: UserWalletId, stakingIds: Set<StakingID>) {
        coroutineScope {
            launch { clearInRuntime(userWalletId = userWalletId, stakingIds = stakingIds) }
            launch { clearInPersistence(userWalletId = userWalletId, stakingIds = stakingIds) }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, values: Set<P2PEthPoolAccountResponse>) {
        val vaults = vaultsProvider()

        val newBalances = values.mapNotNull { response ->
            val vault = vaults.firstOrNull { it.vaultAddress == response.vaultAddress }
            if (vault == null) {
                Timber.w("Vault not found for ${response.vaultAddress}")
                return@mapNotNull null
            }

            val account = P2PEthPoolAccountConverter.convert(response)
            P2PYieldBalanceConverter.convert(
                account = account,
                vault = vault,
                address = account.delegatorAddress,
                source = StatusSource.ACTUAL,
            )
        }.toSet()

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
        val integrationIds = stakingIds.map { it.integrationId }.toSet()

        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = this[userWalletId.stringValue].orEmpty()
                    .filterNot { response ->
                        val responseIntegrationId = "p2p-ethereum-pooled:${response.vaultAddress}"
                        responseIntegrationId in integrationIds
                    }
                    .toSet()
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
}