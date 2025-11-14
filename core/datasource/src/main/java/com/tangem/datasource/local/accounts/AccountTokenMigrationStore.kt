package com.tangem.datasource.local.accounts

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface AccountTokenMigrationStore {

    fun get(userWalletId: UserWalletId): Flow<Pair<String, String>?>

    suspend fun store(userWalletId: UserWalletId, value: Pair<String, String>)

    suspend fun remove(userWalletId: UserWalletId)
}