package com.tangem.datasource.local.userwallet

import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

interface UserWalletsStore {

    val selectedUserWalletOrNull: UserWallet?

    suspend fun getSyncOrNull(key: UserWalletId): UserWallet?
}