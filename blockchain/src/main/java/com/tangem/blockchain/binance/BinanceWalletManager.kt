package com.tangem.blockchain.binance

import android.util.Log
import com.tangem.blockchain.binance.network.BinanceInfoResponse
import com.tangem.blockchain.binance.network.BinanceNetworkManager
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.common.CompletionResult

class BinanceWalletManager(
        private val cardId: String,
        walletPublicKey: ByteArray,
        override var wallet: CurrencyWallet,
        isTestNet: Boolean = false
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain = if (isTestNet) Blockchain.BinanceTestnet else Blockchain.Binance
    private val address = blockchain.makeAddress(walletPublicKey)
    private val transactionBuilder = BinanceTransactionBuilder(walletPublicKey, isTestNet)
    private val networkManager = BinanceNetworkManager(isTestNet)

    init {
        wallet.balances[AmountType.Coin] = Amount(null, blockchain, address)
    }

    override suspend fun update() {
        val result = networkManager.getInfo(address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: BinanceInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.balances[AmountType.Coin]?.value = response.balance

        transactionBuilder.accountNumber = response.accountNumber
        transactionBuilder.sequence = response.sequence
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
        when (val result = networkManager.getFee()) {
            is Result.Success -> return Result.Success(listOf(Amount(result.data, blockchain)))
            is Result.Failure -> return result
        }
    }


}