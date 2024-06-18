package com.tangem.domain.walletconnect.repository

import com.tangem.domain.wallets.models.UserWalletId

interface WalletConnectRepository {

    suspend fun checkIsAvailable(userWalletId: UserWalletId): Boolean
}