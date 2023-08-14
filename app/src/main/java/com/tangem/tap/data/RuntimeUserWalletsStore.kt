package com.tangem.tap.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.firstOrNull

// FIXME: Workaround, remove it once the normal UserWalletsStore has been implemented
// [REDACTED_JIRA]
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
}