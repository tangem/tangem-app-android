package com.tangem.tap.domain.walletregistration

import android.util.Base64
import arrow.core.getOrElse
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.common.visa.VisaUtilities
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.auth.AuthFeatureToggles
import com.tangem.lib.auth.session.WalletNonce
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single entry point that registers wallets with the Tangem Auth Service. Gated by the backend-auth
 * feature toggle; all failures are log-only (registration is retried on the next launch / card scan,
 * never blocks the user). MOBILE wallets register without UI; COLD wallets attest inside a live
 * card session (no extra tap) and POST after the session closes.
 */
class WalletRegistrationLauncher @Inject internal constructor(
    private val walletRegistrar: WalletRegistrar,
    private val mobileSigner: MobileWalletRegistrationSigner,
    private val coldSigner: ColdWalletRegistrationSigner,
    private val authFeatureToggles: AuthFeatureToggles,
    private val hotWalletAccessor: HotWalletAccessor,
    private val appCoroutineScope: AppCoroutineScope,
) {

    /**

     * Never throws (beyond cooperative cancellation) — any unexpected failure is caught and logged,
     * so callers relying on the fire-and-forget contract stay safe.
     */
    suspend fun registerMobile(userWallet: UserWallet.Hot) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return

        runSuspendCatching {
            walletRegistrar.register(
                walletId = userWallet.walletId.toBase64(),
                signer = mobileSigner.signerFor(userWallet),
            ).onLeft { TangemLogger.e("Mobile wallet registration deferred: $it") }
        }.onFailure { TangemLogger.e("Mobile wallet registration failed", it) }
    }

    /**
     * Catches up the registration of an already-saved MOBILE wallet at unlock time. A protected
     * (Password/Biometry) wallet can only register by signing the nonce with its key, which needs an
     * unlock context. Establish a short-lived contextual unlock (the single auth), register within it —
     * so the nonce is requested AFTER the auth and signed immediately (no ~55s expiry) and no second
     * prompt appears — then ALWAYS clear the context. Already-registered wallets are skipped before any
     * auth, so a routine unlock never pops a needless prompt; a context established elsewhere is reused
     * and left untouched.
     */
    suspend fun registerUnlockedMobileWallet(userWallet: UserWallet.Hot) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return
        if (walletRegistrar.isRegistered(userWallet.walletId.toBase64())) return

        if (hotWalletAccessor.getContextualUnlock(userWallet.hotWalletId) != null) {
            // Another flow already holds a context — reuse it (no prompt) and leave it for its owner.
            registerMobile(userWallet)
            return
        }

        try {
            runSuspendCatching { hotWalletAccessor.unlockContextual(userWallet.hotWalletId) }
                .onFailure { error ->
                    TangemLogger.e("Failed to establish an unlock context for mobile registration", error)
                    return
                }
            registerMobile(userWallet)
        } finally {
            hotWalletAccessor.clearContextualUnlock(userWallet.hotWalletId)
        }
    }

    /**
     * COLD nonce prefetch used from `CardSessionRunnable.prepare`, which runs before preflight — so no
     * card is available yet. Requests and deciphers the wallet nonce so its (seconds-long) round-trip
     * can complete before the wrapped card command finishes and the in-session signing needs it.
     *
     * No card-based gating here (already-registered / twin / Visa): those need the card and are still
     * enforced later in [registerColdInSession] and by the registrar's own idempotency check, so at
     * worst an ineligible or already-registered card wastes a nonce.
     *
     * A registrar-side failure is logged and returns `null`; [WalletRegistrar.requestNonce] is total
     * (routes every failure to a `Left`), so this consumes the `Either` directly rather than wrapping it.
     */
    suspend fun prefetchColdNonce(): WalletNonce? {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return null

        return walletRegistrar.requestNonce().getOrElse { error ->
            TangemLogger.e("Cold wallet nonce prefetch deferred: $error")
            null
        }
    }

    /**
     * COLD registration from a [Card] read off an open [session] — applied to any card operation
     * (scan, wallet creation/import, key derivation) so the card is bound to the device on the same
     * NFC tap. Phase 1 ([WalletRegistrar.prepare]) signs the prefetched [nonce] inside the still-open
     * [session] (the card is tapped here); phase 2 (the network POST) is dispatched on
     * [appCoroutineScope] afterwards, so the user doesn't hold the card during the request.
     *
     * Twin and Visa cards are skipped: a twin's id is derived from both cards (can't be built here)
     * and Visa cards are not a cold-registration target.
     *
     * [shouldForce] is for a card that took over a mobile wallet's walletId (hot → cold upgrade): the
     * local marker left by the mobile registration would skip it, so the marker is dropped first — even
     * without a [nonce], letting a later scan register the card — and this tap registers regardless of it.
     */
    suspend fun registerColdInSession(
        session: CardSession,
        card: Card,
        nonce: WalletNonce?,
        shouldForce: Boolean = false,
    ) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return
        val cardDto = CardDTO(card)
        if (cardDto.isTangemTwins || VisaUtilities.isVisaCard(cardDto)) return
        val walletId = UserWalletIdBuilder.card(cardDto).build()?.toBase64() ?: return

        if (shouldForce) {
            walletRegistrar.forgetRegistration(walletId)
                .onLeft { TangemLogger.e("Failed to drop the cold wallet registration marker: $it") }
        }
        if (nonce == null) return

        registerCold(session = session, card = cardDto, walletId = walletId, nonce = nonce, shouldForce = shouldForce)
    }

    private suspend fun registerCold(
        session: CardSession,
        card: CardDTO,
        walletId: String,
        nonce: WalletNonce,
        shouldForce: Boolean,
    ) {
        val prepared = walletRegistrar.prepare(
            walletId = walletId,
            nonce = nonce,
            signer = coldSigner.signerFor(session, card),
            shouldForce = shouldForce,
        ).getOrElse { error ->
            TangemLogger.e("Cold wallet registration prepare deferred: $error")
            return
        }
        if (prepared == null) return // already registered

        appCoroutineScope.launch {
            runSuspendCatching {
                walletRegistrar.submit(prepared)
                    .onLeft { TangemLogger.e("Cold wallet registration submit deferred: $it") }
            }.onFailure { TangemLogger.e("Cold wallet registration submit failed", it) }
        }
    }

    /**
     * Launch-time safety net: registers not-yet-registered MOBILE wallets without any UI.
     *
     * Only wallets that can sign **silently** are retried — i.e. [HotWalletId.AuthType.NoPassword].
     * Password/Biometry wallets would pop an unlock prompt (see `DefaultHotWalletAccessor`), which
     * must never happen at startup; those are left to register when a real unlock context exists
     * (e.g. on creation, or the next time the user unlocks them).
     */
    suspend fun retryMobileRegistrations(userWallets: List<UserWallet>) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return

        userWallets.asSequence()
            .filterIsInstance<UserWallet.Hot>()
            .filter { it.hotWalletId.authType == HotWalletId.AuthType.NoPassword }
            .forEach { registerMobile(it) }
    }

    /**
     * Unregisters a wallet from the auth service (e.g. on wallet deletion). Failures are surfaced
     * through the registrar's total [Either] contract and logged here — no `runSuspendCatching`
     * wrapper, because the registrar never throws (it routes every failure to a `Left`).
     */
    suspend fun unregister(userWalletId: UserWalletId) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return

        walletRegistrar.unregister(userWalletId.toBase64())
            .onLeft { TangemLogger.e("Wallet unregister deferred: $it") }
    }

    private fun UserWalletId.toBase64(): String = Base64.encodeToString(value, Base64.NO_WRAP)
}