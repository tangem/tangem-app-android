package com.tangem.tap.domain.tasks

import com.tangem.CardSession
import com.tangem.CardSessionRunnable
import com.tangem.TangemError
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.TransactionSender
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.commands.CommandResponse
import com.tangem.commands.SignCommand
import com.tangem.commands.SignResponse
import com.tangem.common.CompletionResult
import com.tangem.tap.scope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume

class SendTask(
        private val walletManager: WalletManager,
        private val recipientAddress: String,
        private val amountToSend: Amount,
        private val feeAmount: Amount,
) : CardSessionRunnable<CommandResponse> {

    override val requiresPin2: Boolean = false

    override fun run(session: CardSession, callback: (result: CompletionResult<CommandResponse>) -> Unit) {
        val txSender = walletManager as TransactionSender

        val verifyResult = walletManager.validateTransaction(amountToSend, feeAmount)
        if (verifyResult.isNotEmpty()) {
            callback(CompletionResult.Failure(InsufficientBalance()))
            return
        }
        val txData = walletManager.createTransaction(amountToSend, feeAmount, recipientAddress)

        scope.launch {
            when (val result = txSender.send(txData, SessionTransactionSigner(session))) {
                is SimpleResult.Success -> callback(CompletionResult.Success(SendResponse()))
                is SimpleResult.Failure -> {
                    callback(CompletionResult.Failure(BlockchainInternalErrorConverter.convert(result.error)))
                }
            }
        }
    }
}

class SendResponse : CommandResponse

class SessionTransactionSigner(
        private val session: CardSession
) : TransactionSigner {
    override suspend fun sign(hashes: Array<ByteArray>, cardId: String): CompletionResult<SignResponse> =
            suspendCancellableCoroutine { continuation ->
                Timber.d("sign transaction...")
                SignCommand(hashes).run(session) {
                    if (continuation.isActive) {
                        continuation.resume(it)
                    }
                }
            }
}

abstract class SendError : TangemError {
}

class UnknownError : SendError() {
    override val code: Int = 1000
    override var customMessage: String = "Unknown error"
}

open class ThrowableError(throwable: Throwable?) : SendError() {
    override val code: Int = 1001
    override var customMessage: String = throwable?.localizedMessage ?: "Unknown exception"
}

class InsufficientBalance(
        override var customMessage: String = "Insufficient balance"
) : SendError() {
    override val code: Int = 1021
}

class BlockchainInternalError(
        override var customMessage: String
) : SendError() {
    override val code: Int = 2000
}

class BlockchainInternalErrorConverter {
    companion object {

        private val stellarInternalErrors = mapOf(
                "tx_bad_seq" to "Sequence number does not match source account",
                "tx_too_late" to "The ledger closeTime was after the maxTime",
                "tx_failedop_no_destination" to "The destination account does not exist",
                "tx_no_source_account" to "Source account not found"
        )

        fun convert(throwable: Throwable?): TangemError {
            val message = throwable?.message ?: return ThrowableError(throwable)

            val customMessage = getInternalBlockchainErrorMessage(message)
            return if (customMessage == null) ThrowableError(throwable)
            else BlockchainInternalError(customMessage)
        }

        private fun getInternalBlockchainErrorMessage(message: String): String? {
            return stellarInternalErrors[message]
        }
    }
}