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
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.auth.AuthFeatureToggles
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
     * COLD registration from a [Card] read off an open [session] — applied to any card operation
     * (scan, wallet creation/import, key derivation) so the card is bound to the device on the same
     * NFC tap. Phase 1 ([WalletRegistrar.prepare]) runs inside the still-open [session] (the card is
     * tapped here); phase 2 (the network POST) is dispatched on [appCoroutineScope] afterwards, so
     * the user doesn't hold the card during the request.
     *
     * Twin and Visa cards are skipped: a twin's id is derived from both cards (can't be built here)
     * and Visa cards are not a cold-registration target.
     */
    suspend fun registerColdInSession(session: CardSession, card: Card) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return
        val cardDto = CardDTO(card)
        if (cardDto.isTangemTwins || VisaUtilities.isVisaCard(cardDto)) return
        registerCold(session, cardDto, UserWalletIdBuilder.card(cardDto))
    }

    private suspend fun registerCold(session: CardSession, card: CardDTO, walletIdBuilder: UserWalletIdBuilder) {
        val walletId = walletIdBuilder.build()?.toBase64() ?: return

        val prepared = walletRegistrar.prepare(walletId, coldSigner.signerFor(session, card))
            .getOrElse { error ->
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