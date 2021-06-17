package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.commands.common.card.EllipticCurve
import java.math.BigDecimal


fun Blockchain.isNoAccountError(exception: Throwable): Boolean {
    return when (this) {
        Blockchain.Stellar, Blockchain.XRP ->
            exception.message?.contains("Account not found") == true
        else -> false
    }
}

fun Blockchain.amountToCreateAccount(token: Token? = null): Double? {
    return when (this) {
        Blockchain.Stellar -> if (token?.symbol == NODL) 1.5 else 1.toDouble()
        Blockchain.XRP -> 20.toDouble()
        else -> null
    }
}

fun Blockchain.minimalAmount(): BigDecimal {
    return 1.toBigDecimal().movePointLeft(decimals())
}

fun Blockchain.getSupportedCurves(): List<EllipticCurve>? {
    return when (this) {
        Blockchain.Unknown -> null
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.BitcoinCash, Blockchain.Litecoin,
        Blockchain.Ducatus, Blockchain.Ethereum, Blockchain.EthereumTestnet, Blockchain.RSK,
        Blockchain.XRP, Blockchain.Binance, Blockchain.BinanceTestnet -> listOf(EllipticCurve.Secp256k1)
        Blockchain.Tezos -> listOf(EllipticCurve.Secp256k1, EllipticCurve.Ed25519)
        Blockchain.Cardano, Blockchain.CardanoShelley, Blockchain.Stellar ->
            listOf(EllipticCurve.Ed25519)
        else -> listOf(EllipticCurve.Secp256k1)
    }
}

private const val NODL = "NODL"