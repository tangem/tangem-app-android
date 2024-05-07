package com.tangem.domain.transaction

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.CommonSigner
import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

interface TransactionRepository {

    @Suppress("LongParameterList")
    suspend fun createTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        isSwap: Boolean,
        hash: String?,
    ): TransactionData?

    @Suppress("LongParameterList")
    suspend fun validateTransaction(
        amount: Amount,
        fee: Fee,
        memo: String?,
        destination: String,
        userWalletId: UserWalletId,
        network: Network,
        isSwap: Boolean,
        hash: String?,
    ): Result<Unit>

    suspend fun sendTransaction(
        txData: TransactionData,
        signer: CommonSigner,
        userWalletId: UserWalletId,
        network: Network,
    ): SimpleResult
}
