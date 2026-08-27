package com.tangem.data.polymarket

import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.wallet.UserWallet

/** Returns the wallet's secp256k1 seed public key, or `null` if it has none. */
internal fun UserWallet.secp256k1SeedKey(): ByteArray? = when (this) {
    is UserWallet.Cold -> scanResponse.card.wallets
        .firstOrNull { it.curve == EllipticCurve.Secp256k1 }
        ?.publicKey
    is UserWallet.Hot -> wallets
        ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
        ?.publicKey
}