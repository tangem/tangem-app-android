package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Wraps [inner] and, once it succeeds, performs best-effort COLD wallet registration on the
 * still-open [CardSession] (piggybacking on the same NFC tap), then forwards the original result.
 * Applied by `DefaultTangemSdkManager` to every card operation (except reset and Visa), so a cold
 * card is bound to the device as early as possible instead of waiting for the next scan.
 *
 * On success the inner result is forwarded only **after** the in-session registration attempt
 * completes, so it can extend the operation slightly — but it never changes the outcome: the inner
 * result is always forwarded unchanged, even if registration fails, throws or is cancelled (errors
 * are logged only). All other runnable behaviour (preflight, access-code policy, encryption) is
 * delegated to [inner].
 */
internal class RegisterColdWalletRunnable<T>(
    private val inner: CardSessionRunnable<T>,
    private val launcher: WalletRegistrationLauncher,
    private val sessionScope: CoroutineScope,
) : CardSessionRunnable<T> by inner {

    override fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        inner.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> registerColdThenComplete(session, result, callback)
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    private fun registerColdThenComplete(
        session: CardSession,
        result: CompletionResult.Success<T>,
        callback: (result: CompletionResult<T>) -> Unit,
    ) {
        val card = session.environment.card
        if (card == null) {
            callback(result)
            return
        }
        val job = sessionScope.launch {
            runSuspendCatching { launcher.registerColdInSession(session, card) }
                .onFailure { TangemLogger.e("Cold wallet registration failed", it) }
        }
        // invokeOnCompletion fires even if the job was cancelled or never started (e.g. sessionScope
        // already cancelled), so the wrapped operation's result is always forwarded — the runnable
        // callback can never be dropped, while still waiting for the registration attempt to finish.
        job.invokeOnCompletion { callback(result) }
    }
}