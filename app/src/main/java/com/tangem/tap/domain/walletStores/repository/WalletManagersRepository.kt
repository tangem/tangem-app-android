package com.tangem.tap.domain.walletStores.repository

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.tokens.models.BlockchainNetwork

interface WalletManagersRepository {
    suspend fun findOrMakeMultiCurrencyWalletManager(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork,
    ): CompletionResult<WalletManager>

    suspend fun findOrMakeSingleCurrencyWalletManager(userWallet: UserWallet): CompletionResult<WalletManager>

    suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit>

    suspend fun delete(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
    ): CompletionResult<Unit>

    companion object
}
