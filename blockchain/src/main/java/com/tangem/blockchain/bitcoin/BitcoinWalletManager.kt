package com.tangem.blockchain.bitcoin

import android.util.Log
import com.tangem.blockchain.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.common.extensions.toHexString
import com.tangem.tasks.TaskEvent
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import java.math.BigDecimal

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
    private val currencyWallet = CurrencyWallet(walletConfig, address)
    override var wallet: Wallet = currencyWallet
    private val transactionBuilder = BitcoinTransactionBuilder(isTestNet)
    private val networkManager = BitcoinNetworkManager(isTestNet)

    override suspend fun update() {
        val response = networkManager.getInfo(address)
        when (response) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: BitcoinAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance.toString()}")
        currencyWallet.balances[AmountType.Coin]?.value = response.balance.toBigDecimal()
        transactionBuilder.unspentOutputs = response.unspentTransactions
        if (response.hasUnconfirmed) {
            if (currencyWallet.pendingTransactions.isEmpty()) {
                currencyWallet.pendingTransactions.add(TransactionData(
                        Amount(blockchain.currency, decimals = blockchain.decimals),
                        null,
                        "unknown",
                        currencyWallet.address))
            } else {
                currencyWallet.pendingTransactions.clear()
            }
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
    }


    override suspend fun getEstimateSize(transactionData: TransactionData): Int {
        val transaction: Transaction = transactionData.toBitcoinJTransaction(
                NetworkParameters.fromID(NetworkParameters.ID_MAINNET),
                transactionBuilder.unspentOutputs,
                transactionBuilder.calculateChange(transactionData)
        )
        var size: Int = transaction.unsafeBitcoinSerialize().size
        size += transaction.inputs.sumBy { 130 }
        return size
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val hashes = transactionBuilder.buildToSign(transactionData)
        when (val signerResponse = signer.sign(hashes.toTypedArray(), cardId)) {
            is TaskEvent.Event -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature, walletPublicKey)
                return networkManager.sendTransaction(transactionToSend.toHexString())
            }
            is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, source: String, destination: String): Result<List<Amount>> {
        when (val result = networkManager.getFee()) {
            is Result.Failure -> return result
            is Result.Success -> {
                val bytesInKb = BigDecimal(1024)
                val size = getEstimateSize(TransactionData(amount, null, source, destination)).toBigDecimal()
                val minFee = result.data.minimalPerKb / bytesInKb * size
                val normalFee = result.data.normalPerKb / bytesInKb * size
                val priorityFee = result.data.priorityPerKb / bytesInKb * size
                return Result.Success(
                        listOf(
                                Amount(blockchain.currency,
                                        minFee,
                                        source,
                                        blockchain.decimals),
                                Amount(blockchain.currency,
                                        normalFee,
                                        source,
                                        blockchain.decimals),
                                Amount(blockchain.currency,
                                        priorityFee,
                                        source,
                                        blockchain.decimals)
                        )
                )
            }
        }
    }
}