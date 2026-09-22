package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Wraps [inner] and, once it succeeds, performs best-effort COLD wallet registration on the
 * still-open [CardSession] (piggybacking on the same NFC tap), then forwards the original result.
 * Applied only to the card operations that should bind the card (scan / wallet creation / signing /
 * key derivation) — the call sites decide when to wrap — so a cold card is bound on those taps.
 *
 * The wallet nonce is prefetched in [prepare], which runs *before* the NFC session (no card needed
 * for the nonce), and [prepare] blocks up to [NONCE_PREFETCH_BUDGET_MS] for it. The nonce round-trip
 * is much slower than the wrapped card command, so fetching it before the tap is what lets it be
 * ready in time. Registration then runs only if the nonce has arrived; otherwise this tap skips it
 * (the card binds on a later one).
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

    @Volatile
    private var nonce: WalletNonce? = null

    override fun prepare(session: CardSession, callback: (result: CompletionResult<Unit>) -> Unit) {
        inner.prepare(session) { innerResult ->
            when (innerResult) {
                is CompletionResult.Success -> prefetchNonceThenPrepare(session, innerResult, callback)
                is CompletionResult.Failure -> callback(innerResult)
            }
        }
    }

    private fun prefetchNonceThenPrepare(
        session: CardSession,
        innerResult: CompletionResult<Unit>,
        callback: (result: CompletionResult<Unit>) -> Unit,
    ) {
        // prepare() runs before the NFC session starts, so there is no card yet — but the nonce is
        // card-independent. Fetch it here (blocking up to the budget) so its slow round-trip overlaps
        // the "tap your card" wait instead of the fast card command; a null/timed-out nonce simply
        // skips registration in run(). runSuspendCatching keeps a stray throw from failing the child.
        val job = session.scope.launch {
            nonce = runSuspendCatching {
                withTimeoutOrNull(NONCE_PREFETCH_BUDGET_MS) { launcher.prefetchColdNonce() }
            }.getOrNull()
        }
        // Always forward prepare's result — even if the fetch was cancelled — so the session never hangs.
        job.invokeOnCompletion { callback(innerResult) }
    }

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
        val prefetchedNonce = nonce
        // Register only when the nonce prefetched in prepare() is ready; otherwise skip (the card binds
        // on a later tap). The card is never held waiting for the nonce here — that wait already happened
        // before the tap, in prepare().
        if (card == null || prefetchedNonce == null) {
            callback(result)
            return
        }
        // Launch on the card session's own scope so the in-session registration is bound to the tap
        // (cancelled when the session ends) and can't outlive it.
        val job = session.scope.launch {
            runSuspendCatching { launcher.registerColdInSession(session, card, prefetchedNonce) }
                .onFailure { TangemLogger.e("Cold wallet registration failed", it) }
        }
        // invokeOnCompletion fires even if the job was cancelled or never started (e.g. the session
        // scope was already cancelled), so the wrapped operation's result is always forwarded — the
        // runnable callback can never be dropped, while still waiting for the registration to finish.
        job.invokeOnCompletion { callback(result) }
    }

    private companion object {
        const val NONCE_PREFETCH_BUDGET_MS = 5_000L
    }
}