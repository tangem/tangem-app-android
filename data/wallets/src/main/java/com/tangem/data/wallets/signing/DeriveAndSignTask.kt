package com.tangem.data.wallets.signing

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

internal class DeriveAndSignTask<T>(
    private val scope: CoroutineScope,
    private val derive: suspend (CardSession) -> CompletionResult<ExtendedPublicKey>,
    private val prepare: suspend (ExtendedPublicKey) -> SignRequest<T>,
    private val sign: suspend (CardSession, ExtendedPublicKey, List<ByteArray>) -> CompletionResult<List<ByteArray>>,
) : CardSessionRunnable<Signed<T>> {

    private val isFinished = AtomicBoolean(false)

    override fun run(session: CardSession, callback: CompletionCallback<Signed<T>>) {
        scope.launch {
            val result = runSuspendCatching { deriveAndSign(session) }.getOrElse { error ->
                TangemLogger.e("Derive and sign session: the card session threw", error)
                CompletionResult.Failure(TangemSdkError.ExceptionError(error))
            }
            finish(callback, result)
        }.invokeOnCompletion { cause ->
            if (cause != null) finish(callback, CompletionResult.Failure(TangemSdkError.UserCancelled()))
        }
    }

    fun abandon() {
        isFinished.set(true)
    }

    private suspend fun deriveAndSign(session: CardSession): CompletionResult<Signed<T>> {
        val derivedKey = when (val derived = derive(session)) {
            is CompletionResult.Success -> derived.data
            is CompletionResult.Failure -> return CompletionResult.Failure(derived.error)
        }
        val request = prepare(derivedKey)
        if (request.hashes.isEmpty()) return CompletionResult.Success(Signed(request.value, emptyList()))

        return when (val signed = sign(session, derivedKey, request.hashes)) {
            is CompletionResult.Success -> CompletionResult.Success(Signed(request.value, signed.data))
            is CompletionResult.Failure -> CompletionResult.Failure(signed.error)
        }
    }

    private fun finish(callback: CompletionCallback<Signed<T>>, result: CompletionResult<Signed<T>>) {
        if (isFinished.compareAndSet(false, true)) callback(result)
    }
}