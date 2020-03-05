package com.tangem.blockchain.stellar

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.tasks.TaskEvent
import java.math.BigDecimal
import java.util.*

class StellarWalletManager(
        private val cardId: String,
        walletPublicKey: ByteArray,
        override var wallet: CurrencyWallet,
        isTestNet: Boolean = false
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain: Blockchain = Blockchain.Stellar
    private val address = blockchain.makeAddress(walletPublicKey)
    private val networkManager = StellarNetworkManager(isTestNet)
    private val builder = StellarTransactionBuilder(networkManager, walletPublicKey)
    private var baseFee = BASE_FEE
    private var baseReserve = BASE_RESERVE
    private var sequence = 0L

    override suspend fun update() {
        val result = networkManager.getInfo(address, wallet.balances[AmountType.Token]?.address)
        when (result) {
            is Result.Failure -> updateError(result.error)
            is Result.Success -> updateWallet(result.data)
        }
    }

    private fun updateWallet(data: StellarResponse) {
        wallet.balances[AmountType.Coin]?.value = data.balance
        wallet.balances[AmountType.Token]?.value = data.assetBalance
        wallet.balances[AmountType.Reserve]?.value = data.baseReserve
        sequence = data.sequence
        baseFee = data.baseFee
        baseReserve = data.baseReserve

        val currentTime = Calendar.getInstance().timeInMillis
        wallet.pendingTransactions.forEach { transaction ->
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
        val hashes = builder.buildToSign(transactionData, sequence, baseFee.toStroops())
        when (val signerResponse = signer.sign(hashes.toTypedArray(), cardId)) {
            is TaskEvent.Event -> {
                val transactionToSend = builder.buildToSend(signerResponse.data.signature)
                return networkManager.sendTransaction(transactionToSend)
            }
            is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
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
