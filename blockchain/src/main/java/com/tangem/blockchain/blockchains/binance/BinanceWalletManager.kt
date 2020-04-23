package com.tangem.blockchain.blockchains.binance

import android.util.Log
import com.tangem.blockchain.blockchains.binance.network.BinanceInfoResponse
import com.tangem.blockchain.blockchains.binance.network.BinanceNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult

class BinanceWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: BinanceTransactionBuilder,
        private val networkManager: BinanceNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val result = networkManager.getInfo(wallet.address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: BinanceInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.amounts[AmountType.Coin]?.value = response.balance

        transactionBuilder.accountNumber = response.accountNumber
        transactionBuilder.sequence = response.sequence
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
                when (val signerResponse = signer.sign(arrayOf(buildTransactionResult.data), cardId)) {
                    is CompletionResult.Success -> {
                        val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                        return networkManager.sendTransaction(transactionToSend)
                    }
                    is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
                }
            }
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        when (val result = networkManager.getFee()) {
            is Result.Success -> return Result.Success(listOf(Amount(result.data, blockchain)))
            is Result.Failure -> return result
        }
    }


}