package com.tangem.tap.domain.walletregistration

import android.util.Base64
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.left
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.features.wallet.WalletFeatureToggles
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
import com.tangem.lib.auth.session.WalletRegistrationResult
import com.tangem.tap.domain.walletregistration.BackendAuthAnalyticsEvent.WalletRegistrationFailed
import com.tangem.tap.domain.walletregistration.BackendAuthAnalyticsEvent.WalletRegistrationFailed.Stage
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DefaultWalletDeviceLinkRepository @Inject constructor(
    private val walletRegistrar: WalletRegistrar,
    private val mobileSigner: MobileWalletRegistrationSigner,
    private val coldSigner: ColdWalletRegistrationSigner,
    private val walletFeatureToggles: WalletFeatureToggles,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : WalletDeviceLinkRepository {

    override fun isLinked(userWallet: UserWallet): Flow<Boolean> {
        if (!userWallet.canBeLinked()) return flowOf(true)

        val walletId = userWallet.walletId.toBase64()
        return walletRegistrar.registeredWalletIds()
            .map { registeredWalletIds -> walletId in registeredWalletIds }
            .distinctUntilChanged()
    }

    override suspend fun link(userWallet: UserWallet): Either<WalletLinkError, Unit> {
        if (!userWallet.canBeLinked()) return WalletLinkError.NotSupported.left()

        val walletId = userWallet.walletId.toBase64()
        val result = when (userWallet) {
            is UserWallet.Cold -> registerCold(userWallet, walletId)
            is UserWallet.Hot -> {
                walletRegistrar.register(walletId = walletId, signer = mobileSigner.signerFor(userWallet))
                    .mapLeft { error -> StagedError(Stage.Register, error) }
            }
        }

        return result
            .onRight { registrationResult ->
                if (registrationResult == WalletRegistrationResult.Registered) {
                    analyticsEventHandler.send(BackendAuthAnalyticsEvent.WalletLinked())
                }
            }
            .mapLeft { (stage, error) ->
                error.toLinkError().also { linkError ->
                    if (linkError is WalletLinkError.Failed) {
                        TangemLogger.e("Wallet linking failed at $stage: $error")
                        sendRegistrationFailed(userWallet, stage, linkError)
                    }
                }
            }
            .map { }
    }

    /** Goes through the same phases as the registration on a card scan, so a failure reports the same stages. */
    private suspend fun registerCold(
        userWallet: UserWallet.Cold,
        walletId: String,
    ): Either<StagedError, WalletRegistrationResult> = either {
        if (walletRegistrar.isRegistered(walletId)) return@either WalletRegistrationResult.AlreadyRegistered

        val nonce = walletRegistrar.requestNonce()
            .mapLeft { error -> StagedError(Stage.Nonce, error) }
            .bind()
        val prepared = walletRegistrar
            .prepare(walletId = walletId, nonce = nonce, signer = coldSigner.signerFor(userWallet))
            .mapLeft { error -> StagedError(Stage.Prepare, error) }
            .bind()
            ?: return@either WalletRegistrationResult.AlreadyRegistered

        walletRegistrar.submit(prepared)
            .mapLeft { error -> StagedError(Stage.Submit, error) }
            .bind()
    }

    private fun sendRegistrationFailed(userWallet: UserWallet, stage: Stage, error: WalletLinkError.Failed) {
        val walletType = when (userWallet) {
            is UserWallet.Cold -> WalletRegistrationFailed.WalletType.Cold
            is UserWallet.Hot -> WalletRegistrationFailed.WalletType.Mobile
        }
        analyticsEventHandler.send(
            WalletRegistrationFailed(walletType = walletType, stage = stage, reason = error.reason),
        )
    }

    /**
     * The cold signer signs with the card's first wallet and rejects any curve but secp256k1, so a card whose first
     * wallet is on another curve (an Ed25519-only card) could never be linked and would keep the banner forever.
     */
    private fun UserWallet.canBeLinked(): Boolean {
        if (!walletFeatureToggles.isWalletLinkingEnabled) return false

        return when (this) {
            is UserWallet.Cold -> {
                val card = scanResponse.card
                card.isColdRegistrationSupported && card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1
            }
            is UserWallet.Hot -> true
        }
    }

    private fun WalletRegistrationError.toLinkError(): WalletLinkError {
        return when ((this as? WalletRegistrationError.SigningFailed)?.cause) {
            is TangemSdkError.UserCancelled -> WalletLinkError.Cancelled
            // UserWalletIdPreflightReadFilter rejects a card of another wallet with this error
            is TangemSdkError.WalletNotFound -> WalletLinkError.WrongCard
            else -> WalletLinkError.Failed(reason = toString())
        }
    }

    private fun UserWalletId.toBase64(): String = Base64.encodeToString(value, Base64.NO_WRAP)

    private data class StagedError(val stage: Stage, val error: WalletRegistrationError)
}