package com.tangem.blockchain.blockchains.stellar

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.util.*

class StellarWalletManager(
        cardId: String,
        wallet: Wallet,
        private val transactionBuilder: StellarTransactionBuilder,
        private val networkManager: StellarNetworkManager
) : WalletManager(cardId, wallet), TransactionSender {

    private val blockchain = wallet.blockchain

    private var baseFee = BASE_FEE
    private var baseReserve = BASE_RESERVE
    private var sequence = 0L

    override suspend fun update() {
        val result = networkManager.getInfo(wallet.address, wallet.amounts[AmountType.Token]?.address)
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: StellarResponse) {
        wallet.amounts[AmountType.Coin]?.value = data.balance
        wallet.amounts[AmountType.Token]?.value = data.assetBalance
        wallet.amounts[AmountType.Reserve]?.value = data.baseReserve
        sequence = data.sequence
        baseFee = data.baseFee
        baseReserve = data.baseReserve

        val currentTime = Calendar.getInstance().timeInMillis
        wallet.transactions.forEach { transaction ->
            if (transaction.date?.timeInMillis ?: 0 - currentTime > 10) {
                transaction.status = TransactionStatus.Confirmed
            }
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val hashes = transactionBuilder.buildToSign(transactionData, sequence, baseFee.toStroops())
        when (val signerResponse = signer.sign(hashes.toTypedArray(), cardId)) {
            is CompletionResult.Success -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                return networkManager.sendTransaction(transactionToSend)
            }
            is CompletionResult.Failure -> return SimpleResult.Failure(signerResponse.error)
        }
    }

    override suspend fun getFee(amount: Amount, destination: String): Result<List<Amount>> {
        return Result.Success(listOf(
                Amount(baseFee, blockchain)
        ))
    }

    private fun BigDecimal.toStroops(): Int {
        return this.multiply(STROOPS_IN_XLM).toInt()
    }

    companion object {
        val STROOPS_IN_XLM = 10000000.toBigDecimal()
        val BASE_FEE = 0.00001.toBigDecimal()
        val BASE_RESERVE = 0.5.toBigDecimal()
    }
}

