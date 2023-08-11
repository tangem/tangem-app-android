package com.tangem.domain.common.extensions

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.card.EllipticCurve
import java.math.BigDecimal

@Suppress("ComplexMethod")
fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain? {
    return when (networkId) {
        "arbitrum-one" -> Blockchain.Arbitrum
        "arbitrum-one/test" -> Blockchain.ArbitrumTestnet
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
        "xdai" -> Blockchain.Gnosis
        "ethereum-pow-iou" -> Blockchain.EthereumPow
        "ethereum-pow-iou/test" -> Blockchain.EthereumPowTestnet
        "ethereumfair" -> Blockchain.EthereumFair
        "polkadot" -> Blockchain.Polkadot
        "polkadot/test" -> Blockchain.PolkadotTestnet
        "kusama" -> Blockchain.Kusama
        "optimistic-ethereum" -> Blockchain.Optimism
        "optimistic-ethereum/test" -> Blockchain.OptimismTestnet
        "dash" -> Blockchain.Dash
        "kaspa" -> Blockchain.Kaspa
        "the-open-network" -> Blockchain.TON
        "the-open-network/test" -> Blockchain.TONTestnet
        "kava" -> Blockchain.Kava
        "kava/test" -> Blockchain.KavaTestnet
        "ravencoin" -> Blockchain.Ravencoin
        "ravencoin/test" -> Blockchain.RavencoinTestnet
        "cosmos" -> Blockchain.Cosmos
        "cosmos/test" -> Blockchain.CosmosTestnet
        "terra" -> Blockchain.TerraV1
        "terra-2" -> Blockchain.TerraV2
        "cronos" -> Blockchain.Cronos
        "telos" -> Blockchain.Telos
        "telos/test" -> Blockchain.TelosTestnet
        "aleph-zero" -> Blockchain.AlephZero
        "aleph-zero/test" -> Blockchain.AlephZeroTestnet
        "octaspace" -> Blockchain.OctaSpace
        "octaspace/test" -> Blockchain.OctaSpaceTestnet
        else -> null
    }
}

@Suppress("ComplexMethod")
fun Blockchain.toNetworkId(): String {
    return when (this) {
        Blockchain.Unknown -> "unknown"
        Blockchain.Arbitrum -> "arbitrum-one"
        Blockchain.ArbitrumTestnet -> "arbitrum-one/test"
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
        Blockchain.Gnosis -> "xdai"
        Blockchain.EthereumPow -> "ethereum-pow-iou"
        Blockchain.EthereumPowTestnet -> "ethereum-pow-iou/test"
        Blockchain.EthereumFair -> "ethereumfair"
        Blockchain.Polkadot -> "polkadot"
        Blockchain.PolkadotTestnet -> "polkadot/test"
        Blockchain.Kusama -> "kusama"
        Blockchain.Optimism -> "optimistic-ethereum"
        Blockchain.OptimismTestnet -> "optimistic-ethereum/test"
        Blockchain.Dash -> "dash"
        Blockchain.Kaspa -> "kaspa"
        Blockchain.TON -> "the-open-network"
        Blockchain.TONTestnet -> "the-open-network/test"
        Blockchain.Kava -> "kava"
        Blockchain.KavaTestnet -> "kava/test"
        Blockchain.Ravencoin -> "ravencoin"
        Blockchain.RavencoinTestnet -> "ravencoin/test"
        Blockchain.Cosmos -> "cosmos"
        Blockchain.CosmosTestnet -> "cosmos/test"
        Blockchain.TerraV1 -> "terra"
        Blockchain.TerraV2 -> "terra-2"
        Blockchain.Cronos -> "cronos"
        Blockchain.Telos -> "telos"
        Blockchain.TelosTestnet -> "telos/test"
        Blockchain.AlephZero -> "aleph-zero"
        Blockchain.AlephZeroTestnet -> "aleph-zero/test"
        Blockchain.OctaSpace -> "octaspace"
        Blockchain.OctaSpaceTestnet -> "octaspace/test"
    }
}

@Suppress("ComplexMethod")
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
        Blockchain.Arbitrum, Blockchain.ArbitrumTestnet -> "ethereum"
        Blockchain.Avalanche, Blockchain.AvalancheTestnet -> "avalanche-2"
        Blockchain.Solana, Blockchain.SolanaTestnet -> "solana"
        Blockchain.Fantom, Blockchain.FantomTestnet -> "fantom"
        Blockchain.Tron, Blockchain.TronTestnet -> "tron"
        Blockchain.Polkadot, Blockchain.PolkadotTestnet -> "polkadot"
        Blockchain.Ducatus -> "ducatus"
        Blockchain.Litecoin -> "litecoin"
        Blockchain.RSK -> "rootstock"
        Blockchain.Tezos -> "tezos"
        Blockchain.XRP -> "ripple"
        Blockchain.Dogecoin -> "dogecoin"
        Blockchain.Gnosis -> "xdai"
        Blockchain.EthereumPow, Blockchain.EthereumPowTestnet -> "ethereum-pow-iou"
        Blockchain.EthereumFair -> "ethereumfair"
        Blockchain.Kusama -> "kusama"
        Blockchain.Optimism, Blockchain.OptimismTestnet -> "ethereum"
        Blockchain.Dash -> "dash"
        Blockchain.Kaspa -> "kaspa"
        Blockchain.TON, Blockchain.TONTestnet -> "the-open-network"
        Blockchain.Unknown -> "unknown"
        Blockchain.Kava, Blockchain.KavaTestnet -> "kava"
        Blockchain.Ravencoin, Blockchain.RavencoinTestnet -> "ravencoin"
        Blockchain.Cosmos, Blockchain.CosmosTestnet -> "cosmos"
        Blockchain.TerraV1 -> "terra-luna"
        Blockchain.TerraV2 -> "terra-luna-2"
        Blockchain.Cronos -> "crypto-com-chain"
        Blockchain.Telos, Blockchain.TelosTestnet -> "telos"
        Blockchain.AlephZero, Blockchain.AlephZeroTestnet -> "aleph-zero"
        Blockchain.OctaSpace, Blockchain.OctaSpaceTestnet -> "octaspace"
    }
}

fun Blockchain.isSupportedInApp(): Boolean {
    return !excludedBlockchains.contains(this)
}

fun Blockchain.amountToCreateAccount(token: Token? = null): BigDecimal? {
    return when (this) {
        Blockchain.Stellar -> if (token?.symbol == NODL) BigDecimal(NODL_AMOUNT_TO_CREATE_ACCOUNT) else BigDecimal.ONE
        Blockchain.XRP -> BigDecimal.TEN
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
private const val NODL_AMOUNT_TO_CREATE_ACCOUNT = 1.5

private val excludedBlockchains = listOf(
    Blockchain.Unknown,
    Blockchain.Ducatus,
    Blockchain.Telos, // disable in 4.9
    Blockchain.TelosTestnet, // disable in 4.9
    Blockchain.OctaSpace,
    Blockchain.OctaSpaceTestnet,
)