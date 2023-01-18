package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import java.math.BigDecimal

@Suppress("MagicNumber")
fun Blockchain.amountToCreateAccount(token: Token? = null): BigDecimal? {
    return when (this) {
        Blockchain.Stellar -> if (token?.symbol == NODL) BigDecimal(1.5) else BigDecimal.ONE
        Blockchain.XRP -> BigDecimal(10)
        else -> null
    }
}

fun Blockchain.minimalAmount(): BigDecimal {
    return 1.toBigDecimal().movePointLeft(decimals())
}

fun Blockchain.getPrimaryCurve(): EllipticCurve? {
    return when {
        getSupportedCurves().contains(EllipticCurve.Secp256k1) -> {
            EllipticCurve.Secp256k1
        }
        getSupportedCurves().contains(EllipticCurve.Ed25519) -> {
            EllipticCurve.Ed25519
        }
        else -> {
            null
        }
    }
}

private const val NODL = "NODL"
