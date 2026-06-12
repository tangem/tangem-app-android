package com.tangem.domain.transaction.usecase

import com.tangem.common.card.EllipticCurve
import com.tangem.domain.models.wallet.UserWallet

/**
 * Wallet master secp256k1 public key bytes, without any network derivation. Returns `null` when the
 * wallet is locked or has no secp256k1 key.
 *
 * This is the single source of truth for the key used to sign ([SignHashesUseCase]) and verify
 * ([VerifyMessagesUseCase]) raw hashes, so both operations resolve to the very same key.
 */
internal fun UserWallet.primarySecp256k1PublicKey(): ByteArray? = when (this) {
    is UserWallet.Cold -> scanResponse.card.wallets
        .firstOrNull { it.curve == EllipticCurve.Secp256k1 }
        ?.publicKey
    is UserWallet.Hot -> wallets
        ?.firstOrNull { it.curve == EllipticCurve.Secp256k1 }
        ?.publicKey
}