package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.datasource.local.token.converter.YieldBalanceConverter
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
                storeInRuntimeStore(
                    userWalletId = userWalletId,
                    items = YieldBalanceConverter(isCached = false).convertSet(input = items),
                )
            }
            launch { storeInPersistenceStore(userWalletId = userWalletId, items = items) }
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

        val balances = getSyncOrNull(userWalletId)
            ?.addOrReplace(newBalance) { it.integrationId == integrationId && it.address == address }
            ?: setOf(newBalance)

        storeInRuntimeStore(userWalletId = userWalletId, items = balances)
    }

    private suspend fun storeInRuntimeStore(userWalletId: UserWalletId, items: Set<YieldBalance>) {
        runtimeStore.update(default = emptyMap()) {
            it.toMutableMap().apply {
                this[userWalletId] = items
            }
        }
    }

    private suspend fun storeInPersistenceStore(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>) {
        persistenceStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = items
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
        return runtimeBalances
            .map { runtime ->
                if (runtime is YieldBalance.Error) {
                    cachedBalances.getBalance(address = runtime.address, integrationId = runtime.integrationId)
                        ?: runtime
                } else {
                    runtime
                }
            }
            .toSet()
    }

    private fun Set<YieldBalance>.getBalance(address: String?, integrationId: String?): YieldBalance? {
        return firstOrNull { yieldBalance ->
            val data = yieldBalance as? YieldBalance.Data
            val balance = data?.balance

            val isCorrectAddress = address != null && address == data?.address
            val isCorrectIntegration = integrationId != null && balance?.integrationId == integrationId

            isCorrectIntegration && isCorrectAddress
        }
    }
}