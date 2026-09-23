package com.tangem.data.collectibles.account.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.config.curvesConfig

/**
 * The seed public key the wallet derives its Solana keys from, or `null` if it has none.
 *
 * The curve comes from the wallet's own config rather than a constant — Wallet 2 keeps Solana on
 * `Ed25519Slip0010`, older cards on `Ed25519`. Deriving the owner and signing for it must read the same key, or
 * the signature would come from something other than the `cold_owner` the vault stores.
 */
internal fun UserWallet.solanaSeedKey(): ByteArray? {
    val curve = curvesConfig.primaryCurve(Blockchain.Solana) ?: return null

    return when (this) {
        is UserWallet.Cold -> scanResponse.card.wallets.firstOrNull { it.curve == curve }?.publicKey
        is UserWallet.Hot -> wallets?.firstOrNull { it.curve == curve }?.publicKey
    }
}