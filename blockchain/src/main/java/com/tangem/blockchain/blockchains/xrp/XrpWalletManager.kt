package com.tangem.blockchain.blockchains.xrp

import android.util.Log
import com.tangem.blockchain.blockchains.xrp.network.XrpInfoResponse
import com.tangem.blockchain.blockchains.xrp.network.XrpNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult

class XrpWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: XrpTransactionBuilder,
        private val networkManager: XrpNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val result = networkManager.getInfo(wallet.address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.amounts[AmountType.Reserve]?.value = response.reserveBase

        if (!response.accountFound) {
            updateError(Exception("Account not found")) //TODO rework, add reserve
            return
        }
        wallet.amounts[AmountType.Coin]?.value = response.balance - response.reserveBase
        transactionBuilder.sequence = response.sequence

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
        val transactionHash = transactionBuilder.buildToSign(transactionData)

        when (val signerResponse = signer.sign(arrayOf(transactionHash), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                return networkManager.sendTransaction(transactionToSend)
            }
            is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val result = networkManager.getFee()
        when (result) {
            is Result.Failure -> return result
            is Result.Success -> return Result.Success(listOf(
                    Amount(result.data.minimalFee, blockchain),
                    Amount(result.data.normalFee, blockchain),
                    Amount(result.data.priorityFee, blockchain)
            ))
        }
    }
}