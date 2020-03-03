package com.tangem.blockchain.xrp

import android.util.Log
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.extensions.Result
import com.tangem.blockchain.common.extensions.SimpleResult
import com.tangem.blockchain.wallets.CurrencyWallet
import com.tangem.blockchain.xrp.network.XrpInfoResponse
import com.tangem.blockchain.xrp.network.XrpNetworkManager
import com.tangem.tasks.TaskEvent

class XrpWalletManager(
        private val cardId: String,
        private val walletPublicKey: ByteArray,
        override var wallet: CurrencyWallet
) : WalletManager,
        TransactionSender,
        FeeProvider {

    override val blockchain = Blockchain.XRP
    private val address = blockchain.makeAddress(walletPublicKey)
    private val transactionBuilder = XrpTransactionBuilder(walletPublicKey)
    private val networkManager = XrpNetworkManager()

    init {
        wallet.balances[AmountType.Coin] = Amount(null, blockchain)
        wallet.balances[AmountType.Reserve] = Amount(null, blockchain, type = AmountType.Reserve)
    }

    override suspend fun update() {
        val result = networkManager.getInfo(address)
        when (result) {
            is Result.Success -> updateWallet(result.data)
            is Result.Failure -> updateError(result.error)
        }
    }

    private fun updateWallet(response: XrpInfoResponse) {
        Log.d(this::class.java.simpleName, "Balance is ${response.balance}")
        wallet.balances[AmountType.Reserve]?.value = response.reserveBase

        if (!response.accountFound) {
            updateError(Exception("Account not found")) //TODO rework, add reserve
            return
        }
        wallet.balances[AmountType.Coin]?.value = response.balance - response.reserveBase
        transactionBuilder.sequence = response.sequence

        if (response.hasUnconfirmed) {
            if (wallet.pendingTransactions.isEmpty()) {
                wallet.pendingTransactions.add(TransactionData(
                        Amount(blockchain.currency, decimals = blockchain.decimals),
                        null,
                        "unknown",
                        wallet.address))
            }
        } else {
            wallet.pendingTransactions.clear()
        }
    }

    private fun updateError(error: Throwable?) {
        Log.e(this::class.java.simpleName, error?.message ?: "")
        if (error != null) throw error
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val transactionHash = transactionBuilder.buildToSign(transactionData)

        when (val signerResponse = signer.sign(arrayOf(transactionHash), cardId)) {
            is TaskEvent.Event -> {
                val transactionToSend = transactionBuilder.buildToSend(signerResponse.data.signature)
                return networkManager.sendTransaction(transactionToSend)
            }
            is TaskEvent.Completion -> return SimpleResult.Failure(signerResponse.error)
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