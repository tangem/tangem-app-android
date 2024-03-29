package com.tangem.tap.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

// FIXME: Workaround, remove it once the normal UserWalletsStore has been implemented
// https://tangem.atlassian.net/browse/AND-4110
internal class RuntimeUserWalletsStore(
    private val walletsStateHolder: WalletsStateHolder,
) : UserWalletsStore {

    override val selectedUserWalletOrNull: UserWallet?
        get() = walletsStateHolder.userWalletsListManager?.selectedUserWalletSync

    override suspend fun getSyncOrNull(key: UserWalletId): UserWallet? {
        return walletsStateHolder.userWalletsListManager
            ?.userWallets
            ?.firstOrNull()
            ?.singleOrNull { it.walletId == key }
    }

    override suspend fun getAllSyncOrNull(): List<UserWallet>? {
        return walletsStateHolder.userWalletsListManager
            ?.userWallets
            ?.firstOrNull()
    }

    override suspend fun update(userWalletId: UserWalletId, update: suspend (UserWallet) -> UserWallet) {
        walletsStateHolder.userWalletsListManager?.update(userWalletId, update)
    }
}
