package com.tangem.domain.wallets.legacy

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManager
import com.tangem.common.CompletionResult
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId

interface WalletManagersRepository {

    suspend fun findOrMakeMultiCurrencyWalletManager(
        userWallet: UserWallet,
        blockchainNetwork: BlockchainNetwork,
    ): CompletionResult<WalletManager>

    suspend fun findOrMakeSingleCurrencyWalletManager(userWallet: UserWallet): CompletionResult<WalletManager>

    suspend fun delete(userWalletIds: List<UserWalletId>): CompletionResult<Unit>

    suspend fun delete(userWalletId: UserWalletId, blockchain: Blockchain): CompletionResult<Unit>

    companion object
}
