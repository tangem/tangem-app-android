package com.tangem.blockchain.cardano

import android.util.Base64
import android.util.Log
import com.tangem.blockchain.cardano.network.CardanoAddressResponse
import com.tangem.blockchain.cardano.network.CardanoNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.common.extensions.encodeBase64NoWrap
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.tasks.TaskEvent
import java.math.BigDecimal

class CardanoWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        walletConfig: WalletConfig
) : WalletManager,
        TransactionEstimator,
        TransactionSender,
        FeeProvider {

    override val blockchain = Blockchain.Cardano
    private val address = blockchain.makeAddress(walletPublicKey)
    private val currencyWallet = CurrencyWallet(walletConfig, address)
    override var wallet: Wallet = currencyWallet
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
        currencyWallet.balances[AmountType.Coin]?.value =
                response.balance.toBigDecimal().movePointLeft(blockchain.decimals.toInt())
        transactionBuilder.unspentOutputs = response.unspentOutputs
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
    }

    override suspend fun getEstimateSize(transactionData: TransactionData): Int {
        val dummyFeeValue = BigDecimal.valueOf(0.1)

        val dummyFee = transactionData.amount.copy(value = dummyFeeValue)
        val dummyAmount =
                transactionData.amount.copy(value = transactionData.amount.value!! - dummyFeeValue)

        val dummyTransactionData = transactionData.copy(
                amount = dummyAmount,
                fee = dummyFee
        )
        transactionBuilder.buildToSign(dummyTransactionData)
        return transactionBuilder.buildToSend(ByteArray(64), walletPublicKey).size
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transactionHash = transactionBuilder.buildToSign(transactionData)

        when (val signerResponse = signer.sign(arrayOf(transactionHash), cardId)) {
            is TaskEvent.Event -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature, walletPublicKey)
                return networkManager.sendTransaction(transactionToSend.encodeBase64NoWrap())
            }
            is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, source: String, destination: String): Result<List<Amount>> {
        val a = 0.155381
        val b = 0.000043946
        val size = getEstimateSize(TransactionData(amount, null, source, destination))

        val fee = (a + b * size).toBigDecimal()
        return Result.Success(listOf(Amount(blockchain.currency, fee, source, blockchain.decimals)))
    }
}