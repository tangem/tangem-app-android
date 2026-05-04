package com.tangem.domain.card.configs

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.utils.logging.TangemLogger

object EdSingleCurrencyCardConfig : CardConfig {

    override val mandatoryCurves: List<EllipticCurve> = listOf(EllipticCurve.Ed25519)

    override fun primaryCurve(blockchain: Blockchain): EllipticCurve? {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }
            else -> {
                TangemLogger.e("Unsupported blockchain, curve not found")
                null
            }
        }
    }
}