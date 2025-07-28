package com.tangem.domain.card.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import timber.log.Timber

class GenericCardConfig(maxWalletCount: Int) : CardConfig {

    override val mandatoryCurves: List<EllipticCurve> = buildList {
        add(EllipticCurve.Secp256k1)
        if (maxWalletCount > 1) {
            add(EllipticCurve.Ed25519)
        }
    }

    /**
     * Old logic to determine primary curve for blockchain
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