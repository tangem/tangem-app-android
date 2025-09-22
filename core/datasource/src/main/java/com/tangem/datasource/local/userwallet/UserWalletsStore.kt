package com.tangem.datasource.local.userwallet

import com.tangem.common.CompletionResult
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

@Deprecated(
    message = "Use UserWalletsListRepository instead",
    replaceWith = ReplaceWith("UserWalletsListRepository"),
)
interface UserWalletsStore {

    val selectedUserWalletOrNull: UserWallet?

    val userWallets: Flow<List<UserWallet>>

    val userWalletsSync: List<UserWallet>

    fun getSyncOrNull(key: UserWalletId): UserWallet?

    fun getSyncStrict(key: UserWalletId): UserWallet

    suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet>
}