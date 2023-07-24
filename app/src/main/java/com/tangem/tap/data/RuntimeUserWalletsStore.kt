package com.tangem.tap.data

import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.legacy.WalletsStateHolder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.firstOrNull
// [REDACTED_TODO_COMMENT]
// [REDACTED_JIRA]
internal class RuntimeUserWalletsStore(
    private val walletsStateHolder: WalletsStateHolder,
) : UserWalletsStore {

    override suspend fun getSync(userWalletId: UserWalletId): UserWallet? {
        return walletsStateHolder.userWalletsListManager
            ?.userWallets
            ?.firstOrNull()
            ?.firstOrNull { it.walletId == userWalletId }
    }
}
