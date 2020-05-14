package com.tangem.blockchain.blockchains.bitcoin

import android.util.Log
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinAddressResponse
import com.tangem.blockchain.blockchains.bitcoin.network.BitcoinProvider
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.toHexString
import java.math.BigDecimal

open class BitcoinWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: BitcoinTransactionBuilder,
        private val networkManager: BitcoinProvider
) : WalletManager(cardId, wallet), TransactionSender {

    protected val blockchain = wallet.blockchain

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
        transactionBuilder.unspentOutputs = response.unspentOutputs
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
        when (val buildTransactionResult = transactionBuilder.buildToSign(transactionData)) {
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
        when (val feeResult = networkManager.getFee()) {
            is Result.Failure -> return feeResult
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
                        val minFee = feeResult.data.minimalPerKb.calculateFee(transactionSize)
                        val normalFee = feeResult.data.normalPerKb.calculateFee(transactionSize)
                        val priorityFee = feeResult.data.priorityPerKb.calculateFee(transactionSize)
                        val fees = listOf(Amount(minFee, blockchain),
                                Amount(normalFee, blockchain),
                                Amount(priorityFee, blockchain)
                        )

                        val minimalFee = transactionSize.movePointLeft(blockchain.decimals())
                        for (fee in fees) {
                            if (fee.value!! < minimalFee) fee.value = minimalFee
                        }
                        return Result.Success(fees)
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