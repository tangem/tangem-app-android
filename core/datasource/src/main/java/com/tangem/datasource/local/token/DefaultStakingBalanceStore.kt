package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.entity.YieldBalanceWrappersDTO
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

internal class DefaultStakingBalanceStore(
    private val dataStore: DataStore<YieldBalanceWrappersDTO>,
) : StakingBalanceStore {

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldBalanceWrapperDTO>> {
        return dataStore.data.map { it[userWalletId.stringValue].orEmpty() }
    }

    override suspend fun getSyncOrNull(userWalletId: UserWalletId): Set<YieldBalanceWrapperDTO>? {
        return dataStore.data.firstOrNull()
            ?.get(userWalletId.stringValue)
    }

    override suspend fun store(userWalletId: UserWalletId, items: Set<YieldBalanceWrapperDTO>) {
        dataStore.updateData { current ->
            current.toMutableMap().apply {
                this[userWalletId.stringValue] = items
            }
        }
    }

    override fun get(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): Flow<YieldBalanceWrapperDTO?> {
        return get(userWalletId)
            .map { balances ->
                balances.firstOrNull { it.integrationId == integrationId && it.addresses.address == address }
            }
    }

    override suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        address: String,
        integrationId: String,
    ): YieldBalanceWrapperDTO? {
        return getSyncOrNull(userWalletId)
            ?.firstOrNull { it.integrationId == integrationId && it.addresses.address == address }
    }

    override suspend fun store(
        userWalletId: UserWalletId,
        integrationId: String,
        address: String,
        item: YieldBalanceWrapperDTO,
    ) {
        val balances = getSyncOrNull(userWalletId)
            ?.addOrReplace(item) { it.integrationId == integrationId && it.addresses.address == address }
            ?: setOf(item)

        store(userWalletId, balances)
    }
}