package com.tangem.domain.wallets.repository

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.models.WalletBalanceState

interface WalletBalanceStateRepository {

    suspend fun getWalletBalanceState(userWalletId: UserWalletId): WalletBalanceState?

    suspend fun setWalletBalanceState(userWalletId: UserWalletId, balanceState: WalletBalanceState)
}