package com.tangem.domain.demo

import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.blockchain.common.transaction.TransactionSendResult
import com.tangem.blockchain.common.transaction.TransactionsSendResult
import com.tangem.blockchain.extensions.Result
import com.tangem.common.CompletionResult
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.random.Random

class DemoTransactionSender(private val walletManager: WalletManager) : TransactionSender {

    override suspend fun getFee(amount: Amount, destination: String): Result<TransactionFee> {
        val blockchain = walletManager.wallet.blockchain
        return Result.Success(
            TransactionFee.Choosable(
                minimum = blockchain.getStubFee(minimumFee),
                normal = blockchain.getStubFee(normalFee),
                priority = blockchain.getStubFee(priorityFee),
            ),
        )
    }

    override suspend fun estimateFee(amount: Amount, destination: String): Result<TransactionFee> {
        return getFee(amount, walletManager.wallet.address)
    }

    override suspend fun send(
        transactionData: TransactionData,
        signer: TransactionSigner,
    ): Result<TransactionSendResult> {
        val signerResponse = signer.sign(
            hash = getDataToSign(),
            publicKey = walletManager.wallet.publicKey,
        )
        return when (signerResponse) {
            is CompletionResult.Success -> Result.Failure(Exception(ID).toBlockchainSdkError())
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResponse.error)
        }
    }

    override suspend fun sendMultiple(
        transactionDataList: List<TransactionData>,
        signer: TransactionSigner,
        sendMode: TransactionSender.MultipleTransactionSendMode,
    ): Result<TransactionsSendResult> {
        val signerResponse = signer.sign(
            hash = getDataToSign(),
            publicKey = walletManager.wallet.publicKey,
        )
        return when (signerResponse) {
            is CompletionResult.Success -> Result.Failure(Exception(ID).toBlockchainSdkError())
            is CompletionResult.Failure -> Result.fromTangemSdkError(signerResponse.error)
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

    private fun Blockchain.getStubFee(fee: BigDecimal) = when (this) {
        Blockchain.Ethereum -> Fee.Ethereum.EIP1559(
            amount = Amount(fee, this),
            gasLimit = BigInteger.ONE,
            maxFeePerGas = BigInteger.ONE,
            priorityFee = BigInteger.ONE,
        )
        Blockchain.Dogecoin,
        Blockchain.Bitcoin,
        -> Fee.Bitcoin(
            amount = Amount(fee, this),
            satoshiPerByte = BigDecimal.ONE,
            txSize = BigDecimal.ONE,
        )
        else -> Fee.Common(Amount(normalFee, this))
    }

    companion object {
        val ID: String = DemoTransactionSender::class.java.simpleName

        private val minimumFee = 0.0001.toBigDecimal()
        private val normalFee = 0.0002.toBigDecimal()
        private val priorityFee = 0.0003.toBigDecimal()
    }
}