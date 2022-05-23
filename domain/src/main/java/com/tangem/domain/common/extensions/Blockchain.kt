package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain

fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain? {
    return when (networkId) {
        "avalanche", "avalanche-2" -> Blockchain.Avalanche
        "avalanche/test", "avalanche-2/test" -> Blockchain.AvalancheTestnet
        "binancecoin" -> Blockchain.Binance
        "binancecoin/test" -> Blockchain.BinanceTestnet
        "binance-smart-chain" -> Blockchain.BSC
        "binance-smart-chain/test" -> Blockchain.BSCTestnet
        "ethereum" -> Blockchain.Ethereum
        "ethereum/test" -> Blockchain.EthereumTestnet
        "ethereum-classic" -> Blockchain.EthereumClassic
        "ethereum-classic/test" -> Blockchain.EthereumClassicTestnet
        "polygon-pos", "matic-network" -> Blockchain.Polygon
        "polygon-pos/test", "matic-network/test" -> Blockchain.PolygonTestnet
        "solana" -> Blockchain.Solana
        "solana/test" -> Blockchain.SolanaTestnet
        "fantom" -> Blockchain.Fantom
        "fantom/test" -> Blockchain.FantomTestnet
        "bitcoin" -> Blockchain.Bitcoin
        "bitcoin/test" -> Blockchain.BitcoinTestnet
        "bitcoin-cash" -> Blockchain.BitcoinCash
        "bitcoin-cash/test" -> Blockchain.BitcoinCashTestnet
        "cardano" -> Blockchain.CardanoShelley
        "dogecoin" -> Blockchain.Dogecoin
        "ducatus" -> Blockchain.Ducatus
        "litecoin" -> Blockchain.Litecoin
        "rootstock" -> Blockchain.RSK
        "stellar" -> Blockchain.Stellar
        "stellar/test" -> Blockchain.StellarTestnet
        "tezos" -> Blockchain.Tezos
        "tron" -> Blockchain.Tron
        "tron/test" -> Blockchain.TronTestnet
        "xrp", "ripple" -> Blockchain.XRP
        else -> null
    }
}

fun Blockchain.toNetworkId(): String {
    return when (this) {
        Blockchain.Unknown -> "unknown"
        Blockchain.Avalanche -> "avalanche"
        Blockchain.AvalancheTestnet -> "avalanche/test"
        Blockchain.Binance -> "binancecoin"
        Blockchain.BinanceTestnet -> "binancecoin/test"
        Blockchain.BSC -> "binance-smart-chain"
        Blockchain.BSCTestnet -> "binance-smart-chain/test"
        Blockchain.Bitcoin -> "bitcoin"
        Blockchain.BitcoinTestnet -> "bitcoin/test"
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.BitcoinCashTestnet -> "bitcoin-cash/test"
        Blockchain.Cardano -> "cardano"
        Blockchain.CardanoShelley -> "cardano"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Ethereum -> "ethereum"
        Blockchain.EthereumTestnet -> "ethereum/test"
        Blockchain.EthereumClassic -> "ethereum-classic"
        Blockchain.EthereumClassicTestnet -> "ethereum-classic/test"
        Blockchain.Fantom -> "fantom"
        Blockchain.FantomTestnet -> "fantom/test"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.Polygon -> "polygon-pos"
        Blockchain.PolygonTestnet -> "polygon-pos/test"
        Blockchain.RSK -> "rootstock"
        Blockchain.Stellar -> "stellar"
        Blockchain.StellarTestnet -> "stellar/test"
        Blockchain.Solana -> "solana"
        Blockchain.SolanaTestnet -> "solana/test"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "xrp"
        Blockchain.Tron -> "tron"
        Blockchain.TronTestnet -> "tron/test"
    }
}

fun Blockchain.toCoinId(): String {
    return when (this) {
        Blockchain.Binance, Blockchain.BinanceTestnet, Blockchain.BSC, Blockchain.BSCTestnet -> "binancecoin"
        Blockchain.Bitcoin, Blockchain.BitcoinTestnet -> "bitcoin"
        Blockchain.BitcoinCash, Blockchain.BitcoinCashTestnet -> "bitcoin-cash"
        Blockchain.Ethereum, Blockchain.EthereumTestnet -> "ethereum"
        Blockchain.EthereumClassic, Blockchain.EthereumClassicTestnet -> "ethereum-classic"
        Blockchain.Stellar, Blockchain.StellarTestnet -> "stellar"
        Blockchain.Cardano, Blockchain.CardanoShelley -> "cardano"
        Blockchain.Polygon, Blockchain.PolygonTestnet -> "matic-network"
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> "avalanche-2"
        Blockchain.Solana, Blockchain.SolanaTestnet -> "solana"
        Blockchain.Fantom, Blockchain.FantomTestnet -> "fantom"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.RSK -> "rootstock"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "ripple"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Tron, Blockchain.TronTestnet -> "tron"
        else -> "unknown"
    }
}