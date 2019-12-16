package com.tangem.blockchain.common

import com.tangem.commands.SignResponse
import com.tangem.tasks.TaskEvent

interface WalletManager {
    var wallet: Wallet
    val blockchain: Blockchain

    fun update()
}

interface TransactionEstimator {
    fun getEstimateSize(transactionData: TransactionData): Int
}

interface TransactionSender {
    fun send(transactionData: TransactionData, signer: TransactionSigner)
}

interface TransactionSigner {
    fun sign(hashes: Array<ByteArray>, cardId: String,
             callback: (result: TaskEvent<SignResponse>) -> Unit)
}

interface FeeProvider {
    fun getFee(amount: Amount, source: String, destination: String): List<Amount>
}