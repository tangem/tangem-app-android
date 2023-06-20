package com.tangem.tap.domain.userWalletList.repository

import com.tangem.domain.models.userwallet.UserWalletId

internal interface SelectedUserWalletRepository {
    fun get(): UserWalletId?
    fun set(walletId: UserWalletId?)
}
