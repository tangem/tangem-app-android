package com.tangem.datasource.local.walletmanager

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.flow.Flow

interface WalletManagersStore {

    fun getAll(userWalletId: UserWalletId): Flow<List<WalletManager>>

    suspend fun getSyncOrNull(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        derivationPath: String?,
    ): WalletManager?

    suspend fun getAllSync(userWalletId: UserWalletId): List<WalletManager>

    suspend fun store(userWalletId: UserWalletId, walletManager: WalletManager)

    suspend fun remove(userWalletId: UserWalletId, predicate: (WalletManager) -> Boolean)

    suspend fun clear()
}