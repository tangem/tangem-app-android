package com.tangem.tap.domain.tasks.product

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.tap.domain.walletregistration.WalletRegistrationLauncher
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch

/**
 * A card operation that must run the in-session COLD wallet registration itself, at a point where the card is
 * still being read, because the operation ends with a step that leaves the NFC session paused — the scan's
 * card attestation pauses it, after which the card can't be reached (the tag goes stale and re-taps are
 * ignored). [RegisterColdWalletRunnable] hands such an operation the registration via [coldWalletRegistration]
 * instead of registering after it.
 */
internal interface ColdWalletRegistrationHost {
    var coldWalletRegistration: InSessionColdWalletRegistration?
}

internal fun interface InSessionColdWalletRegistration {

    /** Registers on [session]; [onComplete] is always called, whatever the outcome, so the host can proceed. */
    fun register(session: CardSession, onComplete: () -> Unit)
}

/**
 * Wraps [inner] and performs best-effort COLD wallet registration on the still-open [CardSession]
 * (piggybacking on the same NFC tap). Applied only to the card operations that should bind the card
 * (scan / wallet creation / signing / key derivation) — the call sites decide when to wrap — so a cold
 * card is bound on those taps.
 *
 * The registration runs once [inner] succeeds, before its result is forwarded — unless [inner] is a
 * [ColdWalletRegistrationHost], which is handed the registration to run mid-operation instead.
 *
 * The wallet nonce is prefetched in [prepare], which runs *before* the NFC session (no card needed
 * for the nonce), and [prepare] blocks for it within the launcher's time budget. The nonce round-trip
 * is much slower than the wrapped card command, so fetching it before the tap is what lets it be
 * ready in time. Registration then runs only if the nonce has arrived; otherwise this tap skips it
 * (the card binds on a later one).
 *
 * The registration can extend the operation slightly, but it never changes the outcome: the operation
 * always continues, even if registration fails, throws or is cancelled (errors are logged only). All
 * other runnable behaviour (preflight, access-code policy, encryption) is delegated to [inner].
 */
internal class RegisterColdWalletRunnable<T>(
    private val inner: CardSessionRunnable<T>,
    private val launcher: WalletRegistrationLauncher,
    private val shouldForceRegistration: Boolean = false,
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
        // card-independent. Fetch it here (the launcher caps the wait) so its slow round-trip overlaps
        // the "tap your card" wait instead of the fast card command; a null/timed-out nonce simply
        // skips registration in run(). runSuspendCatching keeps a stray throw from failing the child.
        val job = session.scope.launch {
            nonce = runSuspendCatching { launcher.prefetchColdNonce() }.getOrNull()
        }
        // Always forward prepare's result — even if the fetch was cancelled — so the session never hangs.
        job.invokeOnCompletion { callback(innerResult) }
    }

    override fun run(session: CardSession, callback: (result: CompletionResult<T>) -> Unit) {
        if (inner is ColdWalletRegistrationHost) {
            inner.coldWalletRegistration = InSessionColdWalletRegistration(::registerCold)
            inner.run(session, callback)
            return
        }

        inner.run(session) { result ->
            when (result) {
                is CompletionResult.Success -> registerCold(session) { callback(result) }
                is CompletionResult.Failure -> callback(result)
            }
        }
    }

    private fun registerCold(session: CardSession, onComplete: () -> Unit) {
        val card = session.environment.card
        val prefetchedNonce = nonce
        // Register only when the nonce prefetched in prepare() is ready; otherwise skip (the card binds
        // on a later tap). The card is never held waiting for the nonce here — that wait already happened
        // before the tap, in prepare(). A forced registration still goes through without a nonce, since it
        // must drop the stale local marker for that later tap to register.
        val hasRegistrationWork = prefetchedNonce != null || shouldForceRegistration
        if (card == null || !hasRegistrationWork) {
            onComplete()
            return
        }
        // Launch on the card session's own scope so the in-session registration is bound to the tap
        // (cancelled when the session ends) and can't outlive it.
        val job = session.scope.launch {
            runSuspendCatching {
                launcher.registerColdInSession(
                    session = session,
                    card = card,
                    nonce = prefetchedNonce,
                    shouldForce = shouldForceRegistration,
                )
            }
                .onFailure { TangemLogger.e("Cold wallet registration failed", it) }
        }
        // invokeOnCompletion fires even if the job was cancelled or never started (e.g. the session
        // scope was already cancelled), so the operation always proceeds — it can never be left hanging,
        // while still waiting for the registration to finish.
        job.invokeOnCompletion { onComplete() }
    }
}