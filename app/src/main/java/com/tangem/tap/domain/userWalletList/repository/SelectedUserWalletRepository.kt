package com.tangem.tap.domain.userWalletList.repository

import com.tangem.domain.wallets.models.UserWalletId

internal interface SelectedUserWalletRepository {
    suspend fun get(): UserWalletId?
    suspend fun set(walletId: UserWalletId?)
}