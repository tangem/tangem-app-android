package com.tangem.domain.walletconnect.repository

import com.tangem.domain.models.wallet.UserWalletId

interface WalletConnectRepository {

    suspend fun checkIsAvailable(userWalletId: UserWalletId): Boolean
}