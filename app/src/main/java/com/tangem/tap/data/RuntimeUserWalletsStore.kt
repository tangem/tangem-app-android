package com.tangem.tap.data

import com.tangem.common.CompletionResult
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.UserWalletsListManager
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

// FIXME: Workaround, remove it once the normal UserWalletsStore has been implemented
// [REDACTED_JIRA]
internal class RuntimeUserWalletsStore(
    private val userWalletsListManager: UserWalletsListManager,
) : UserWalletsStore {

    override val selectedUserWalletOrNull: UserWallet?
        get() = userWalletsListManager.selectedUserWalletSync

    override val userWallets: Flow<List<UserWallet>>
        get() = userWalletsListManager.userWallets

    override fun getSyncOrNull(key: UserWalletId): UserWallet? {
        return userWalletsListManager.userWalletsSync.firstOrNull { it.walletId == key }
    }

    override suspend fun getSyncStrict(key: UserWalletId): UserWallet {
        return requireNotNull(getSyncOrNull(key)) { "Unable to find user wallet with provided ID: $key" }
    }

    override suspend fun getAllSyncOrNull(): List<UserWallet>? {
        return userWalletsListManager.userWallets.firstOrNull()
    }

    override suspend fun update(
        userWalletId: UserWalletId,
        update: suspend (UserWallet) -> UserWallet,
    ): CompletionResult<UserWallet> {
        return userWalletsListManager.update(userWalletId, update)
    }
}