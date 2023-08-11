package com.tangem.domain.common.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import timber.log.Timber

object Wallet2CardConfig : CardConfig {
    override val mandatoryCurves: List<EllipticCurve>
        get() = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bip0340,
            EllipticCurve.Bls12381G2Aug,
            EllipticCurve.Ed25519Slip0010,
        )

    /**
     * Logic to determine primary curve for blockchain in TangemWallet 2.0
     */
    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519Slip0010) -> {
                EllipticCurve.Ed25519Slip0010
            }
            blockchain.getSupportedCurves().contains(EllipticCurve.Bls12381G2Aug) -> {
                EllipticCurve.Bls12381G2Aug
            }
            else -> {
                Timber.e("Unsupported blockchain, curve not found")
                null
            }
        }
    }
}
