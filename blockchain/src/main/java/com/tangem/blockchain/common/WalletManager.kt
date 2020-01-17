package com.tangem.blockchain.common

import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.commands.SignResponse
import com.tangem.tasks.TaskEvent

interface WalletManager {
    var wallet: Wallet
    val blockchain: Blockchain

    suspend fun update()
}

interface TransactionEstimator {
    suspend fun getEstimateSize(transactionData: TransactionData): Int
}

interface TransactionSender {
    suspend fun send(transactionData: TransactionData, signer: TransactionSigner) : SimpleResult
}

interface TransactionSigner {
    suspend fun sign(hashes: Array<ByteArray>, cardId: String): TaskEvent<SignResponse>
}

interface FeeProvider {
    suspend fun getFee(amount: Amount, source: String, destination: String): Result<List<Amount>>
}