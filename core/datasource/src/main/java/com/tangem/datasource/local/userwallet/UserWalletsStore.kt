package com.tangem.datasource.local.userwallet

import com.tangem.common.CompletionResult
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow

interface UserWalletsStore {

    val selectedUserWalletOrNull: UserWallet?

    val userWallets: Flow<List<UserWallet>>

    fun getSyncOrNull(key: UserWalletId): UserWallet?

    fun getSyncStrict(key: UserWalletId): UserWallet

    suspend fun getAllSyncOrNull(): List<UserWallet>?

    suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet>
}