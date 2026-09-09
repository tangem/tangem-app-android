package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/**
 * Wraps [inner] and, once it succeeds, performs best-effort COLD wallet registration on the
 * still-open [CardSession] (piggybacking on the same NFC tap), then forwards the original result.
 * Applied only to the card operations that should bind the card (scan / wallet creation / signing /
 * key derivation) — the call sites decide when to wrap — so a cold card is bound on those taps.
 *
 * The wallet nonce is prefetched at the start of [run] (the card is already read by then, so
 * [WalletRegistrationLauncher.requestColdNonce] can skip the request for an already-registered card)
 * and its network round-trip overlaps the wrapped card command. Registration then runs only if the
 * nonce has already arrived; otherwise this tap skips it (the card binds on a later one).
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
) : CardSessionRunnable<T> by inner {

    override fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        // Preflight has already read the card by the time run() starts, so requestColdNonce can
        // identify it and skip the network request for an already-registered card. Kicked off here so
        // the nonce round-trip overlaps the wrapped command. A wallet-creating task replaces the first
        // wallet, so its preflight id is stale — don't gate registration on it.
        val shouldCheckRegistered = inner !is CreateProductWalletTask
        val nonceDeferred = session.environment.card?.let { card ->
            session.scope.async { launcher.requestColdNonce(card, shouldCheckRegistered) }
        }

        inner.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> registerColdThenComplete(
                    session = session,
                    nonceDeferred = nonceDeferred,
                    result = result,
                    callback = callback,
                )
                is CompletionResult.Failure -> {
                    nonceDeferred?.cancel()
                    callback(result)
                }
            }
        }
    }

    private fun registerColdThenComplete(
        session: CardSession,
        nonceDeferred: Deferred<WalletNonce?>?,
        result: CompletionResult.Success<T>,
        callback: (result: CompletionResult<T>) -> Unit,
    ) {
        val card = session.environment.card
        val nonce = nonceDeferred?.completedNonceOrNull()
        // Register only when the prefetched nonce has already arrived — never hold the card waiting
        // for a slow nonce. If it isn't ready (or there's no card), skip: the card binds on a later tap.
        if (card == null || nonce == null) {
            // Drop a still-in-flight prefetch — its result won't be used on this tap.
            nonceDeferred?.cancel()
            callback(result)
            return
        }
        // Launch on the card session's own scope so the in-session registration is bound to the tap
        // (cancelled when the session ends) and can't outlive it.
        val job = session.scope.launch {
            runSuspendCatching { launcher.registerColdInSession(session, card, nonce) }
                .onFailure { TangemLogger.e("Cold wallet registration failed", it) }
        }
        // invokeOnCompletion fires even if the job was cancelled or never started (e.g. the session
        // scope was already cancelled), so the wrapped operation's result is always forwarded — the
        // runnable callback can never be dropped, while still waiting for the registration to finish.
        job.invokeOnCompletion { callback(result) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Deferred<WalletNonce?>.completedNonceOrNull(): WalletNonce? =
        // getCompleted() throws if the deferred was cancelled or completed exceptionally, so guard it:
        // a prefetch failure must never escape here and break forwarding the wrapped result.
        if (isCompleted) runCatching { getCompleted() }.getOrNull() else null
}