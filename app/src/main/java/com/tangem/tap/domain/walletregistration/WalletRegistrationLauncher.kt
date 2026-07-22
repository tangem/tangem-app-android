package com.tangem.tap.domain.walletregistration

import android.util.Base64
import arrow.core.getOrElse
import com.tangem.common.core.CardSession
import com.tangem.domain.models.scan.ScanResponse
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
     * COLD registration. Phase 1 ([WalletRegistrar.prepare]) runs inside the still-open [session]
     * (the card is tapped here); phase 2 (the network POST) is dispatched on [appCoroutineScope]
     * after this returns, so the user doesn't hold the card during the request. Call this BEFORE
     * the scan completes its session callback.
     */
    suspend fun registerColdInSession(session: CardSession, scanResponse: ScanResponse) {
        if (!authFeatureToggles.isBackendAuthenticationEnabled) return

        val walletId = UserWalletIdBuilder.scanResponse(scanResponse).build()?.toBase64() ?: return

        val prepared = walletRegistrar.prepare(walletId, coldSigner.signerFor(session, scanResponse))
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

    private fun UserWalletId.toBase64(): String = Base64.encodeToString(value, Base64.NO_WRAP)
}