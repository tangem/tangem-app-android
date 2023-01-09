package com.tangem.tap.domain.userWalletList.repository

import com.tangem.domain.common.util.UserWalletId

internal interface SelectedUserWalletRepository {
    fun get(): UserWalletId?
    fun set(walletId: UserWalletId?)
}
