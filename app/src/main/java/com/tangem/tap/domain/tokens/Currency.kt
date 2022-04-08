package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain

@JsonClass(generateAdapter = true)
data class CurrencyFromJson(
    val id: String,
    val name: String,
    val symbol: String,
    val contracts: List<ContractFromJson>? = null
)

@JsonClass(generateAdapter = true)
data class ContractFromJson(
    val networkId: String,
    val address: String,
    val decimalCount: Int
)

@JsonClass(generateAdapter = true)
data class CurrenciesFromJson(
    val imageHost: String,
    val tokens: List<CurrencyFromJson>
)


fun List<ContractFromJson>.toContracts(): List<Contract> {
    return map { Contract.fromJsonObject(it) }
}

data class Currency(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val contracts: List<Contract>?
) {

    companion object {
        fun fromJsonObject(currency: CurrencyFromJson): Currency {
            return Currency(
                id = currency.id,
                name = currency.name,
                symbol = currency.symbol,
                iconUrl = getIconUrl(currency.id),
                contracts = currency.contracts?.toContracts()
            )
        }
    }
}

data class Contract(
    val networkId: String,
    val blockchain: Blockchain,
    val address: String,
    val decimalCount: Int,
    val iconUrl: String,
) {

    companion object {
        fun fromJsonObject(contract: ContractFromJson): Contract {
            return Contract(
                networkId = contract.networkId,
                blockchain = Blockchain.fromNetworkId(contract.networkId),
                address = contract.address,
                decimalCount = contract.decimalCount,
                iconUrl = getIconUrl(contract.networkId)
            )
        }
    }
}

fun getIconUrl(id: String): String {
    return "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/$id.png"
}


fun Blockchain.Companion.fromNetworkId(networkId: String): Blockchain {
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
        else -> Blockchain.Unknown
    }
}

fun Blockchain.toNetworkId(): String {
    return when (this) {
        Blockchain.Unknown -> "unknown"
        Blockchain.Avalanche -> "avalanche"
        Blockchain.AvalancheTestnet -> "avalaunch"
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