package com.tangem.blockchain.bitcoin

import com.tangem.blockchain.common.*
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.tasks.TaskEvent
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction

class BitcoinWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        walletConfig: WalletConfig,
        isTestNet: Boolean = false
) : WalletManager,
        TransactionEstimator,
        TransactionSender,
        FeeProvider {

    override val blockchain = if (isTestNet) Blockchain.BitcoinTestnet else Blockchain.Bitcoin
    private val address = blockchain.makeAddress(walletPublicKey)
    override var wallet: Wallet = CurrencyWallet(walletConfig, address)
    private val transactionBuilder = BitcoinTransactionBuilder(isTestNet)


    override fun update() {
        transactionBuilder.unspentOutputs = listOf()
    }


    override fun getEstimateSize(transactionData: TransactionData): Int {
        val transaction: Transaction = transactionData.toBitcoinJTransaction(
                NetworkParameters.fromID(NetworkParameters.ID_MAINNET),
                transactionBuilder.unspentOutputs,
                transactionBuilder.calculateChange(transactionData)
                )
        var size: Int = transaction.unsafeBitcoinSerialize().size
        size += transaction.inputs.sumBy { 130 }
        return size
    }

    override fun send(transactionData: TransactionData, signer: TransactionSigner) {
        val hashes = transactionBuilder.buildToSign(transactionData)
        signer.sign(hashes.toTypedArray(), cardId) {
            when (it) {
                is TaskEvent.Event -> transactionBuilder.buildToSend(it.data.signature, walletPublicKey)
            }
        }
    }

    override fun getFee(amount: Amount, source: String, destination: String): List<Amount> {
        return BitcoinServer.getFee()
    }
}