package com.tangem.tap.domain.tasks

import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionEncryption
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class PreparingSignHashesTask(
    private val hashesProvider: suspend () -> List<ByteArray>,
    private val publicKey: Wallet.PublicKey,
    private val pairWalletPublicKey: ByteArray?,
) : CardSessionRunnable<TangemSignHashesResponse> {

    override fun run(session: CardSession, callback: CompletionCallback<TangemSignHashesResponse>) {
        val isFinished = AtomicBoolean(false)
        val finish: CompletionCallback<TangemSignHashesResponse> = { result ->
            if (isFinished.compareAndSet(false, true)) callback(result)
        }

        establishSecureChannel(session) { authorization ->
            when (authorization) {
                is CompletionResult.Success -> prepareAndSign(session, finish)
                is CompletionResult.Failure -> finish(CompletionResult.Failure(authorization.error))
            }
        }
    }

    private fun establishSecureChannel(session: CardSession, callback: CompletionCallback<Unit>) {
        session.establishEncryptionIfNeeded(
            cardSessionEncryption = CardSessionEncryption.SECURE_CHANNEL,
            shouldAskForAccessCode = true,
            callback = callback,
        )
    }

    private fun prepareAndSign(session: CardSession, finish: CompletionCallback<TangemSignHashesResponse>) {
        session.scope.launch {
            val hashes = runSuspendCatching { hashesProvider() }.getOrElse { error ->
                // UserCancelled closes the NFC sheet without a card error; the caller reports the preparation error
                TangemLogger.e("Failed to prepare hashes for signing", error)
                finish(CompletionResult.Failure(TangemSdkError.UserCancelled()))
                return@launch
            }
            SignHashesTask(hashes, publicKey, pairWalletPublicKey).run(session, finish)
        }.invokeOnCompletion { cause ->
            when (cause) {
                null -> Unit
                is CancellationException -> finish(CompletionResult.Failure(TangemSdkError.UserCancelled()))
                else -> finish(CompletionResult.Failure(TangemSdkError.ExceptionError(cause)))
            }
        }
    }
}