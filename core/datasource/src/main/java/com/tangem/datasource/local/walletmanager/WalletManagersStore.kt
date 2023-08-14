package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.wallets.models.UserWalletId

interface WalletManagersStore {

    suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager?

    suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager)

    suspend fun clear()
}