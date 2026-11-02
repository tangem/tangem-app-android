package com.tangem.tap.domain.walletregistration

import android.util.Base64
import arrow.core.Either
import arrow.core.left
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.registration.WalletDeviceLinkRepository
import com.tangem.domain.wallets.registration.WalletLinkError
import com.tangem.lib.auth.session.WalletRegistrar
import com.tangem.lib.auth.session.WalletRegistrationError
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

        val signer = when (userWallet) {
            is UserWallet.Cold -> coldSigner.signerFor(userWallet)
            is UserWallet.Hot -> mobileSigner.signerFor(userWallet)
        }

        return walletRegistrar.register(walletId = userWallet.walletId.toBase64(), signer = signer)
            .mapLeft { error ->
                error.toLinkError().also { linkError ->
                    if (linkError is WalletLinkError.Failed) TangemLogger.e("Wallet linking failed: $error")
                }
            }
    }

    /**
     * The cold signer signs with the card's first wallet and rejects any curve but secp256k1, so a card whose first
     * wallet is on another curve (an Ed25519-only card) could never be linked and would keep the banner forever.
     */
    private fun UserWallet.canBeLinked(): Boolean = when (this) {
        is UserWallet.Cold -> {
            val card = scanResponse.card
            card.isColdRegistrationSupported && card.wallets.firstOrNull()?.curve == EllipticCurve.Secp256k1
        }
        is UserWallet.Hot -> true
    }

    private fun WalletRegistrationError.toLinkError(): WalletLinkError {
        val isCancelled = this is WalletRegistrationError.SigningFailed && cause is TangemSdkError.UserCancelled
        return if (isCancelled) WalletLinkError.Cancelled else WalletLinkError.Failed(reason = toString())
    }

    private fun UserWalletId.toBase64(): String = Base64.encodeToString(value, Base64.NO_WRAP)
}