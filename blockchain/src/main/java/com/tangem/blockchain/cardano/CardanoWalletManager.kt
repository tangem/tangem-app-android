package com.tangem.blockchain.cardano

import android.util.Log
import com.tangem.blockchain.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.encodeBase64NoWrap
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.common.CompletionResult

class CardanoWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        override var wallet: CurrencyWallet
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain = Blockchain.Cardano
    private val address = blockchain.makeAddress(walletPublicKey)
    private val transactionBuilder = CardanoTransactionBuilder()
    private val networkManager = CardanoNetworkManager()

    override suspend fun update() {
        val response = networkManager.getInfo(address)
        when (response) {
            is Result.Success -> updateWallet(response.data)
            is Result.Failure -> updateError(response.error)
        }
    }

    private fun updateWallet(response: CardanoAddressResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance.toString()}")
        wallet.balances[AmountType.Coin]?.value =
                response.balance.toBigDecimal().movePointLeft(blockchain.decimals.toInt())
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
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature, walletPublicKey)
                return networkManager.sendTransaction(transactionToSend.encodeBase64NoWrap())
            }
            is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        val a = 0.155381
        val b = 0.000043946
        val size = transactionBuilder.getEstimateSize(
                TransactionData(amount, null, address, destination), walletPublicKey
        )
        val fee = (a + b * size).toBigDecimal()
        return Result.Success(listOf(Amount(blockchain.currency, fee, address, blockchain.decimals)))
    }
}