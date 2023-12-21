package com.tangem.tap.features.demo

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.extensions.Result
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.common.CompletionResult
import kotlin.random.Random

class DemoTransactionSender(private val walletManager: WalletManager) : TransactionSender {

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val blockchain = walletManager.wallet.blockchain
        return Result.Success(
            TransactionFee.Choosable(
                minimum = Fee.Common(Amount(minimumFee, blockchain)),
                normal = Fee.Common(Amount(normalFee, blockchain)),
                priority = Fee.Common(Amount(priorityFee, blockchain)),
            ),
        )
    }

    override suspend fun estimateFee(amount: Amount, destination: String): Result<TransactionFee> {
        return getFee(amount, walletManager.wallet.address)
    }

    override suspend fun send(transactionData: TransactionData, signer: TransactionSigner): SimpleResult {
        val signerResponse = signer.sign(
            hash = getDataToSign(),
            publicKey = walletManager.wallet.publicKey,
        )
        return when (signerResponse) {
            is CompletionResult.Success -> SimpleResult.Failure(Exception(ID).toBlockchainSdkError())
            is CompletionResult.Failure -> SimpleResult.fromTangemSdkError(signerResponse.error)
        }
    }

    private fun getDataToSign(): ByteArray {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return IntRange(start = 1, endInclusive = 32)
            .map { Random.nextInt(from = 0, until = charPool.size) }
            .map(charPool::get)
            .joinToString(separator = "")
            .toByteArray()
    }

    companion object {
        val ID: String = DemoTransactionSender::class.java.simpleName

        private val minimumFee = 0.0001.toBigDecimal()
        private val normalFee = 0.0002.toBigDecimal()
        private val priorityFee = 0.0003.toBigDecimal()
    }
}
