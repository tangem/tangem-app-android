package com.tangem.datasource.local.token

import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultStakingBalanceStore(
    private val dataStore: StringKeyDataStore<Set<YieldBalanceWrapperDTO>>,
) : StakingBalanceStore {

    private val mutex = Mutex()

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldBalanceWrapperDTO>> {
        return dataStore.get(userWalletId.stringValue)
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalanceWrapperDTO>? {
        return dataStore.getSyncOrNull(userWalletId.stringValue)
    }

    override suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>) {
        mutex.withLock {
            dataStore.store(userWalletId.stringValue, items)
        }
    }

    override fun get(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): Flow<YieldBalanceWrapperDTO?> {
        return dataStore.get(userWalletId.stringValue)
            .map { balances ->
                balances.firstOrNull { it.integrationId == integrationId && it.addresses.address == address }
            }
    }

    override suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): YieldBalanceWrapperDTO? {
        return dataStore.getSyncOrNull(userWalletId.stringValue)
            ?.firstOrNull { it.integrationId == integrationId && it.addresses.address == address }
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        integrationId: String,
        address: String,
        item: YieldBalanceWrapperDTO,
    ) {
        mutex.withLock {
            val balances = dataStore.getSyncOrNull(userWalletId.stringValue)
                ?.addOrReplace(item) { it.integrationId == integrationId && it.addresses.address == address }
                ?: setOf(item)

            dataStore.store(userWalletId.stringValue, balances)
        }
    }
}
