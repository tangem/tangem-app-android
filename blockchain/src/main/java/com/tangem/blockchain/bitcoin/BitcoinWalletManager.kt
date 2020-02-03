package com.tangem.blockchain.bitcoin

import android.util.Log
import com.tangem.blockchain.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.bitcoin.network.BitcoinNetworkManager.Companion.SATOSHI_IN_BTC
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.common.extensions.toHexString
import com.tangem.tasks.TaskEvent
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
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        currencyWallet.balances[AmountType.Coin]?.value = response.balance
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

    override suspend fun getEstimateSize(transactionData: TransactionData): Result<Int> {
        val buildTransactionResult = transactionBuilder.buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return buildTransactionResult
            is Result.Success -> {
                val hashes = buildTransactionResult.data
                val finalTransaction = transactionBuilder.buildToSend(ByteArray(64 * hashes.size) {1}, walletPublicKey)
                return Result.Success(finalTransaction.size)
            }
        }
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val buildTransactionResult = transactionBuilder.buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                when (val signerResponse = signer.sign(buildTransactionResult.data.toTypedArray(), cardId)) {
                    is TaskEvent.Event -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature, walletPublicKey)
                        return networkManager.sendTransaction(transactionToSend.toHexString())
                    }
                    is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, source: String, destination: String): Result<List<Amount>> {
        when (val result = networkManager.getFee()) {
            is Result.Failure -> return result
            is Result.Success -> {
                val sizeResult = getEstimateSize(
                        TransactionData(amount,
                                Amount(1.toBigDecimal().divide(SATOSHI_IN_BTC), blockchain),
                                address, destination)
                )
                when (sizeResult) {
                    is Result.Failure -> return sizeResult
                    is Result.Success -> {
                        val transactionSize = sizeResult.data.toBigDecimal()
                        val minFee = result.data.minimalPerKb.calculateFee(transactionSize)
                        val normalFee = result.data.normalPerKb.calculateFee(transactionSize)
                        val priorityFee = result.data.priorityPerKb.calculateFee(transactionSize)
                        return Result.Success(
                                listOf(Amount(minFee, blockchain),
                                        Amount(normalFee, blockchain),
                                        Amount(priorityFee, blockchain))
                        )
                    }
                }
            }
        }
    }

    private fun BigDecimal.calculateFee(transactionSize: BigDecimal): BigDecimal {
        val bytesInKb = BigDecimal(1024)
        return this.divide(bytesInKb).multiply(transactionSize)
                .setScale(8, blockchain.roundingMode())
    }
}