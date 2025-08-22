package com.tangem.tap.domain.userWalletList.repository

import com.tangem.domain.models.wallet.UserWalletId

internal interface SelectedUserWalletRepository {
    suspend fun get(): UserWalletId?
    suspend fun set(walletId: UserWalletId?)
}