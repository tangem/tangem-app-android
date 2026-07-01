package com.tangem.lib.crypto.derivation

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Checks whether this [EllipticCurve] is able to derive the given [path].
 *
 * `ed25519` and `ed25519_slip0010` support hardened derivation only (SLIP-0010), so any path that contains a
 * non-hardened node cannot produce a key/address for them. This is the root cause of the "custom token added without
 * an address" bug: e.g. an Algorand (ed25519) token with an EVM derivation path like `m/44'/60'/0'/0/0`.
 *
 * `secp256k1`, `secp256r1` and `bip0340` support both hardened and non-hardened derivation, so any path is fine.
 *
 * BLS curves do not support derivation at all.
 */
fun EllipticCurve.supportsDerivationPath(path: DerivationPath): Boolean {
    return when (this) {
        EllipticCurve.Ed25519,
        EllipticCurve.Ed25519Slip0010,
        -> path.nodes.all { it.isHardened }
        EllipticCurve.Secp256k1,
        EllipticCurve.Secp256r1,
        EllipticCurve.Bip0340,
        -> true
        EllipticCurve.Bls12381G2,
        EllipticCurve.Bls12381G2Aug,
        EllipticCurve.Bls12381G2Pop,
        -> false
    }
}