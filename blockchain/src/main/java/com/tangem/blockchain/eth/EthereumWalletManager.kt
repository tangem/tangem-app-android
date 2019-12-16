package com.tangem.blockchain.eth

import com.tangem.blockchain.common.*
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.tasks.TaskEvent

class EthereumWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        walletConfig: WalletConfig
) : WalletManager,
        TransactionEstimator,
        TransactionSender,
        FeeProvider {

    override val blockchain: Blockchain = Blockchain.Ethereum
    private val address = blockchain.makeAddress(walletPublicKey)
    override var wallet: Wallet = CurrencyWallet(walletConfig, address)

    override fun update() {

    }

    override fun getEstimateSize(transactionData: TransactionData): Int {

    }

    override fun send(transactionData: TransactionData, signer: TransactionSigner) {
        val builder = EthereumTransactionBuilder()
        val hashes = builder.buildToSign(transactionData, walletPublicKey)
        signer.sign(hashes.toTypedArray(), cardId) {
            when (it) {
                is TaskEvent.Event -> builder.buildToSend(transactionData, it.data.signature)
            }
        }
    }

    override fun getFee(amount: Amount, source: String, destination: String): List<Amount> {

    }

}


class EthereumTransactionBuilder {


    fun buildToSign(transactionData: TransactionData, publicKey: ByteArray): List<ByteArray> {

    }

    fun buildToSend(transactionData: TransactionData, signature: ByteArray) {

    }
}
