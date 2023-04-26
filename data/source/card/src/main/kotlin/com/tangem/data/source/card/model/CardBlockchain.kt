package com.tangem.data.source.card.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.Card
import com.tangem.common.card.EllipticCurve
import com.tangem.data.source.card.utils.derivationStyle

internal class CardBlockchain(
    private val blockchain: Blockchain,
    private val card: Card,
) {
    val primaryCurve: EllipticCurve? get() {
        return when {
            blockchain.getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
                EllipticCurve.Secp256k1
            }
            blockchain.getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
                EllipticCurve.Ed25519
            }
            else -> {
                null
            }
        }
    }

    val derivationPath: String? get() {
        return if (card.settings.isHDWalletAllowed) {
            blockchain.derivationPath(card.derivationStyle)?.rawPath
        } else {
            null
        }
    }
}
