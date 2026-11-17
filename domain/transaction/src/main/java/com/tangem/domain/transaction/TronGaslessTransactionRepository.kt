package com.tangem.domain.transaction

import com.tangem.blockchain.common.TransactionData
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.models.tron.TronGaslessApproveEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessQuote
import com.tangem.domain.transaction.models.tron.TronGaslessSubmitResult
import com.tangem.domain.transaction.models.tron.TronGaslessSwapEstimateParams
import com.tangem.domain.transaction.models.tron.TronGaslessToken

interface TronGaslessTransactionRepository {
    suspend fun getSupportedTokens(): List<TronGaslessToken>
    suspend fun estimate(params: TronGaslessEstimateParams): TronGaslessQuote
    suspend fun estimateSwap(params: TronGaslessSwapEstimateParams): TronGaslessQuote
    suspend fun estimateApprove(params: TronGaslessApproveEstimateParams): TronGaslessQuote
    suspend fun submit(
        userWalletId: UserWalletId,
        quoteId: String,
        signedCompensationTx: String,
        signedOriginalTx: String,
    ): TronGaslessSubmitResult

    /**
     * Builds + signs (one signing session, no broadcast) the two Tron gasless transactions and returns
     * each as a TronWeb-compatible signed-JSON string in input order. Returns `null` if the wallet
     * manager is unavailable, the network is not Tron (the manager does not implement the gasless
     * signing contract), or signing fails.
     */
    suspend fun sign(
        userWalletId: UserWalletId,
        network: Network,
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
    ): List<String>?
}