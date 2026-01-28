package com.tangem.datasource.local.accounts

import com.tangem.domain.models.account.AccountName
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AccountTokenMigrationStore {

    fun get(userWalletId: UserWalletId): Flow<Pair<AccountName, AccountName>?>

    suspend fun store(userWalletId: UserWalletId, value: Pair<AccountName, AccountName>)

    suspend fun remove(userWalletId: UserWalletId)
}