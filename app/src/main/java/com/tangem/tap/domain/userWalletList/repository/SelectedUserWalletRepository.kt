package com.tangem.tap.domain.userWalletList.repository

import com.tangem.domain.wallets.models.UserWalletId

internal interface SelectedUserWalletRepository {
    fun get(): UserWalletId?
    fun set(walletId: UserWalletId?)
}