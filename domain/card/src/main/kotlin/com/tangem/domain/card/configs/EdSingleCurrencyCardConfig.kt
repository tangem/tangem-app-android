package com.tangem.domain.card.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import timber.log.Timber

object EdSingleCurrencyCardConfig : CardConfig {

    override val mandatoryCurves: List<EllipticCurve> = listOf(EllipticCurve.Ed25519)

    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return when {
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