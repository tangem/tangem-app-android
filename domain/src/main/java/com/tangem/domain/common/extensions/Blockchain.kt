package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain

fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain? {
    return when (networkId) {
        "avalanche" -> Blockchain.Avalanche
        "binancecoin" -> Blockchain.Binance
        "binance-smart-chain" -> Blockchain.BSC
        "ethereum" -> Blockchain.Ethereum
        "polygon-pos" -> Blockchain.Polygon
        "solana" -> Blockchain.Solana
        "fantom" -> Blockchain.Fantom
        "bitcoin" -> Blockchain.Bitcoin
        "bitcoin-cash" -> Blockchain.BitcoinCash
        "cardano" -> Blockchain.CardanoShelley
        "dogecoin" -> Blockchain.Dogecoin
        "ducatus" -> Blockchain.Ducatus
        "litecoin" -> Blockchain.Litecoin
        "rsk" -> Blockchain.RSK
        "stellar" -> Blockchain.Stellar
        "tezos" -> Blockchain.Tezos
        "ripple" -> Blockchain.XRP
        else -> null
    }
}

fun Blockchain.toNetworkId(): String {
    return when (this) {
        Blockchain.Unknown -> "unknown"
        Blockchain.Avalanche -> "avalanche"
        Blockchain.AvalancheTestnet -> "avalaunche"
        Blockchain.Binance -> "binancecoin"
        Blockchain.BinanceTestnet -> "binancecoin"
        Blockchain.BSC -> "binance-smart-chain"
        Blockchain.BSCTestnet -> "binance-smart-chain"
        Blockchain.Bitcoin -> "bitcoin"
        Blockchain.BitcoinTestnet -> "bitcoin"
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.BitcoinCashTestnet -> "bitcoin-cash"
        Blockchain.Cardano -> "cardano"
        Blockchain.CardanoShelley -> "cardano"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Ethereum -> "ethereum"
        Blockchain.EthereumTestnet -> "ethereum"
        Blockchain.Fantom -> "fantom"
        Blockchain.FantomTestnet -> "fantom"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.Polygon -> "matic-network"
        Blockchain.PolygonTestnet -> "matic-networks"
        Blockchain.RSK -> "rootstock"
        Blockchain.Stellar -> "stellar"
        Blockchain.StellarTestnet -> "stellar"
        Blockchain.Solana -> "solana"
        Blockchain.SolanaTestnet -> "solana"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "ripple"
    }
}