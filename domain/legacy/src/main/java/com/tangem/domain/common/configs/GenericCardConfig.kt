package com.tangem.domain.common.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import timber.log.Timber

object GenericCardConfig : CardConfig {
    override val mandatoryCurves: List<EllipticCurve>
        get() = listOf(
            EllipticCurve.Secp256k1,
            EllipticCurve.Ed25519,
            EllipticCurve.Bip0340,
            EllipticCurve.Bls12381G2Aug,
        )

    /**
     * Old logic to determine primary curve for blockchain in TangemWallet
     */
    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }
            else -> {
                Timber.e("Unsupported blockchain, curve not found")
                null
            }
        }
    }
}
