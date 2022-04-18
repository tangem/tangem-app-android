package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain

fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain? {
    return when (networkId) {
        "avalanche", "avalanche-2" -> Blockchain.Avalanche
        "avalanche-testnet", "avalanche-2-testnet" -> Blockchain.AvalancheTestnet
        "binancecoin" -> Blockchain.Binance
        "binancecoin-testnet" -> Blockchain.BinanceTestnet
        "binance-smart-chain" -> Blockchain.BSC
        "binance-smart-chain-testnet" -> Blockchain.BSCTestnet
        "ethereum" -> Blockchain.Ethereum
        "ethereum-testnet" -> Blockchain.EthereumTestnet
        "polygon-pos" -> Blockchain.Polygon
        "polygon-pos-testnet" -> Blockchain.PolygonTestnet
        "solana" -> Blockchain.Solana
        "solana-testnet" -> Blockchain.SolanaTestnet
        "fantom" -> Blockchain.Fantom
        "fantom-testnet" -> Blockchain.FantomTestnet
        "bitcoin" -> Blockchain.Bitcoin
        "bitcoin-testnet" -> Blockchain.BitcoinTestnet
        "bitcoin-cash" -> Blockchain.BitcoinCash
        "bitcoin-cash-testnet" -> Blockchain.BitcoinCashTestnet
        "cardano" -> Blockchain.CardanoShelley
        "dogecoin" -> Blockchain.Dogecoin
        "ducatus" -> Blockchain.Ducatus
        "litecoin" -> Blockchain.Litecoin
        "rsk" -> Blockchain.RSK
        "stellar" -> Blockchain.Stellar
        "stellar-testnet" -> Blockchain.StellarTestnet
        "tezos" -> Blockchain.Tezos
        "ripple" -> Blockchain.XRP
        else -> null
    }
}

fun Blockchain.toNetworkId(): String {
    return when (this) {
        Blockchain.Unknown -> "unknown"
        Blockchain.Avalanche -> "avalanche"
        Blockchain.AvalancheTestnet -> "avalanche-testnet"
        Blockchain.Binance -> "binancecoin"
        Blockchain.BinanceTestnet -> "binancecoin-testnet"
        Blockchain.BSC -> "binance-smart-chain"
        Blockchain.BSCTestnet -> "binance-smart-chain-testnet"
        Blockchain.Bitcoin -> "bitcoin"
        Blockchain.BitcoinTestnet -> "bitcoin-testnet"
        Blockchain.BitcoinCash -> "bitcoin-cash"
        Blockchain.BitcoinCashTestnet -> "bitcoin-cash-testnet"
        Blockchain.Cardano -> "cardano"
        Blockchain.CardanoShelley -> "cardano"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Ethereum -> "ethereum"
        Blockchain.EthereumTestnet -> "ethereum-testnet"
        Blockchain.Fantom -> "fantom"
        Blockchain.FantomTestnet -> "fantom-testnet"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.Polygon -> "matic-network"
        Blockchain.PolygonTestnet -> "matic-networks-testnet"
        Blockchain.RSK -> "rootstock"
        Blockchain.Stellar -> "stellar"
        Blockchain.StellarTestnet -> "stellar-testnet"
        Blockchain.Solana -> "solana"
        Blockchain.SolanaTestnet -> "solana-testnet"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "ripple"
    }
}