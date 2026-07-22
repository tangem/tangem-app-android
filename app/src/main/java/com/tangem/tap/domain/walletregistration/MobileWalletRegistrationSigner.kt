package com.tangem.tap.domain.walletregistration

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.DataToSign
import com.tangem.lib.auth.session.WalletSignatureBundle
import com.tangem.lib.auth.session.WalletSigner
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Builds a [WalletSigner] for a MOBILE (hot/software) wallet. The wallet's secp256k1 key signs
 * `sha256(nonceBytes || walletSignatureSalt)` via the hot wallet SDK; there is no card, so the
 * card-signature fields stay null.
 */
internal class MobileWalletRegistrationSigner @Inject constructor(
    private val hotWalletAccessor: HotWalletAccessor,
) {

    fun signerFor(userWallet: UserWallet.Hot): WalletSigner = WalletSigner { nonceBytes ->
        val wallet = userWallet.wallets
            ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
            ?: error("No secp256k1 wallet available for hot wallet ${userWallet.walletId}")

        val salt = ByteArray(SALT_SIZE).also(secureRandom::nextBytes)
        val hash = (nonceBytes + salt).calculateSha256()

        val signature = hotWalletAccessor.signHashes(
            hotWalletId = userWallet.hotWalletId,
            dataToSign = listOf(DataToSign(curve = EllipticCurve.Secp256k1, hashes = listOf(hash))),
        ).first().signatures.first()

        val rsvSignature = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = hash,
            publicKey = wallet.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM()

        WalletSignatureBundle(
            walletSignature = rsvSignature,
            walletSignatureSalt = salt,
            cardSignature = null,
            cardSignatureSalt = null,
            walletStatusByte = null,
        )
    }

    private companion object {
        const val SALT_SIZE = 16
        val secureRandom = SecureRandom()
    }
}