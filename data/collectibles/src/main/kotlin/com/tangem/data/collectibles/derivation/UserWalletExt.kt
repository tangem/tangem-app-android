package com.tangem.data.collectibles.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.curvesConfig

/**
 * Returns the seed public key the wallet derives its Solana keys from, or `null` if it has none.
 *
 * The curve comes from the wallet's own config rather than being hardcoded: Wallet 2 cards keep Solana on
 * `Ed25519Slip0010`, older ones on plain `Ed25519`. `Blockchain.Solana` stands in for the devnet too — both map
 * to the same curve.
 */
internal fun UserWallet.solanaSeedKey(): ByteArray? {
    val curve = curvesConfig.primaryCurve(Blockchain.Solana) ?: return null

    return when (this) {
        is UserWallet.Cold -> scanResponse.card.wallets.firstOrNull { it.curve == curve }?.publicKey
        is UserWallet.Hot -> wallets?.firstOrNull { it.curve == curve }?.publicKey
    }
}