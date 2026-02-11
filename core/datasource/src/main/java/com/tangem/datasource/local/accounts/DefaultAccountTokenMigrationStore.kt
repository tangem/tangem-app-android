package com.tangem.datasource.local.accounts

import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultAccountTokenMigrationStore(
    private val runtimeStateStore: RuntimeStateStore<Map<UserWalletId, Pair<AccountName, AccountName>>>,
) : AccountTokenMigrationStore {

    override fun get(userWalletId: UserWalletId): Flow<Pair<AccountName, AccountName>?> {
        return runtimeStateStore.get().map { it[userWalletId] }
    }

    override suspend fun store(userWalletId: UserWalletId, value: Pair<AccountName, AccountName>) {
        runtimeStateStore.update { it + (userWalletId to value) }
    }

    override suspend fun remove(userWalletId: UserWalletId) {
        runtimeStateStore.update { it - userWalletId }
    }
}