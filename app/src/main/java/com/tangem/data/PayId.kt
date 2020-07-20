package com.tangem.data

import java.util.*

private val payIdSupported = EnumSet.of(
        Blockchain.Ripple,
        Blockchain.Ethereum,
        Blockchain.Bitcoin,
        Blockchain.Token,
        Blockchain.Litecoin,
        Blockchain.Stellar,
        Blockchain.StellarAsset,
        Blockchain.Cardano,
        Blockchain.Ducatus,
        Blockchain.BitcoinCash,
        Blockchain.Binance,
        Blockchain.BinanceAsset,
        Blockchain.Rootstock,
        Blockchain.RootstockToken,
        Blockchain.Tezos,
        Blockchain.Eos,
        Blockchain.Matic
)

fun Blockchain.isPayIdSupported(): Boolean {
    return payIdSupported.contains(this)
}

fun Blockchain.getPayIdNetwork(): String {
    return when (this) {
        Blockchain.Ripple -> "XRPL"
        Blockchain.Rootstock, Blockchain.RootstockToken -> "RSK"
        else -> this.currency
    }

}