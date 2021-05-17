package com.tangem.tap.domain.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.commands.common.card.EllipticCurve
import org.stellar.sdk.requests.ErrorResponse
import java.math.BigDecimal


fun Blockchain.isNoAccountError(exception: Throwable): Boolean {
    return when (this) {
        Blockchain.Stellar -> {
            (exception is ErrorResponse) && (exception.code == 404)
        }
        Blockchain.XRP -> exception.message?.contains("Account not found") == true
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

fun Blockchain.getCurve(): EllipticCurve? {
    return when (this) {
        Blockchain.Unknown -> null
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet, Blockchain.BitcoinCash, Blockchain.Litecoin,
        Blockchain.Ducatus, Blockchain.Ethereum, Blockchain.EthereumTestnet, Blockchain.RSK,
        Blockchain.Tezos, Blockchain.XRP, Blockchain.Binance, Blockchain.BinanceTestnet ->
            EllipticCurve.Secp256k1
        Blockchain.Cardano, Blockchain.CardanoShelley, Blockchain.Stellar -> EllipticCurve.Ed25519
    }
}

private const val NODL = "NODL"
