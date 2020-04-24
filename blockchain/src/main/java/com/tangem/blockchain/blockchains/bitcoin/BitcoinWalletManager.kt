package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

class BitcoinWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: BitcoinTransactionBuilder,
        private val networkManager: BitcoinNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val response = networkManager.getInfo(wallet.address)
        when (response) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: BitcoinAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.amounts[AmountType.Coin]?.value = response.balance
        transactionBuilder.unspentOutputs = response.unspentTransactions
        if (response.hasUnconfirmed) {
            if (wallet.transactions.isEmpty()) wallet.addIncomingTransaction()
        } else {
            wallet.transactions.clear()
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val buildTransactionResult = transactionBuilder.buildToSign(transactionData)
        when (buildTransactionResult) {
            is Result.Failure -> return SimpleResult.Failure(buildTransactionResult.error)
            is Result.Success -> {
                when (val signerResponse = signer.sign(buildTransactionResult.data.toTypedArray(), cardId)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                        return networkManager.sendTransaction(transactionToSend.toHexString())
                    }
                    is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        when (val result = networkManager.getFee()) {
            is Result.Failure -> return result
            is Result.Success -> {
                val feeValue = BigDecimal.ONE.movePointLeft(blockchain.decimals())
                amount.value = amount.value!! - feeValue
                val sizeResult = transactionBuilder.getEstimateSize(
                        TransactionData(amount, Amount(amount, feeValue), wallet.address, destination)
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
                .setScale(8, BigDecimal.ROUND_DOWN)
    }
}