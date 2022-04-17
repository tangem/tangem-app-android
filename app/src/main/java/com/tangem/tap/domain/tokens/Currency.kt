package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.extensions.fromNetworkId

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
    val imageHost: String?,
    val tokens: List<CurrencyFromJson>
)


fun List<ContractFromJson>.toContracts(isTestNet: Boolean): List<Contract> {
    return mapNotNull { Contract.fromJsonObject(it, isTestNet) }
}

data class Currency(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val contracts: List<Contract>?
) {

    companion object {
        fun fromJsonObject(currency: CurrencyFromJson, isTestNet: Boolean): Currency {
            return Currency(
                id = currency.id,
                name = currency.name,
                symbol = currency.symbol,
                iconUrl = getIconUrl(currency.id),
                contracts = currency.contracts?.toContracts(isTestNet)
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
        fun fromJsonObject(contract: ContractFromJson, isTestNet: Boolean): Contract? {
            val networkId = if (isTestNet) contract.networkId + TESTNET else contract.networkId
            val blockchain = Blockchain.fromNetworkId(networkId) ?: return null
            return Contract(
                networkId = networkId,
                blockchain = blockchain,
                address = contract.address,
                decimalCount = contract.decimalCount,
                iconUrl = getIconUrl(contract.networkId)
            )
        }

        const val TESTNET = "-testnet"
    }
}

fun getIconUrl(id: String): String {
    return "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/$id.png"
}