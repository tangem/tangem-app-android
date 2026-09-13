package com.tangem.lib.crypto.derivation

import com.tangem.common.card.EllipticCurve
import com.tangem.crypto.hdWallet.DerivationPath

/**
 * Checks whether this [EllipticCurve] is able to derive the given [path].
 *
 * `ed25519_slip0010` supports hardened derivation only, so a path containing a non-hardened node cannot produce a
 * key/address for it — e.g. an Algorand token with an EVM derivation path like `m/44'/60'/0'/0/0`. `ed25519` is a
 * different scheme (Ikarus / BIP32-Ed25519, see `AnyMasterKeyFactory`) and does support non-hardened nodes, which
 * Cardano relies on: its CIP-1852 path is `m/1852'/1815'/0'/0/0`. The card enforces the same distinction — see
 * `DeriveWalletPublicKeyTask`.
 *
 * `secp256k1`, `secp256r1` and `bip0340` support both hardened and non-hardened derivation, so any path is fine.
 *
 * BLS curves do not support derivation at all.
 */
fun EllipticCurve.supportsDerivationPath(path: DerivationPath): Boolean {
    return when (this) {
        EllipticCurve.Ed25519Slip0010 -> path.nodes.all { it.isHardened }
        EllipticCurve.Ed25519,
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