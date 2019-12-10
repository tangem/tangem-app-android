package com.tangem.blockchain.common

interface WalletManager {
    var wallet: Wallet
    val blockchain: Blockchain

    fun update()
}

interface TransactionBuilder {
    fun getEstimateSize(transaction: Transaction): Int
}

interface TransactionSender {
    fun send(transaction: Transaction, signer: TransactionSigner)
}

interface TransactionSigner

interface FeeProvider {
    fun getFee(amount: Amount, source: String, destination: String): List<Amount>
}