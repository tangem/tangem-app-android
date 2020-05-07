package com.tangem.blockchain.blockchains.cardano

import android.util.Log
import com.tangem.blockchain.blockchains.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.blockchains.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.blockchain.extensions.encodeBase64NoWrap
import com.tangem.common.CompletionResult
import java.math.RoundingMode

class CardanoWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: CardanoTransactionBuilder,
        private val networkManager: CardanoNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    override suspend fun update() {
        val response = networkManager.getInfo(wallet.address)
        when (response) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance.toString()}")
        wallet.amounts[AmountType.Coin]?.value =
                response.balance.toBigDecimal().movePointLeft(blockchain.decimals())
        transactionBuilder.unspentOutputs = response.unspentOutputs
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
                return networkManager.sendTransaction(transactionToSend.encodeBase64NoWrap())
            }
            is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val a = 0.155381
        val b = 0.000043946
        val size = transactionBuilder.getEstimateSize(
                TransactionData(amount, null, wallet.address, destination)
        )
        val fee = (a + b * size).toBigDecimal().setScale(blockchain.decimals(), RoundingMode.UP)
        return Result.Success(listOf(Amount(amount, fee)))
    }
}