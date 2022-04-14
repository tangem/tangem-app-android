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
    val imageHost: String,
    val tokens: List<CurrencyFromJson>
)


fun List<ContractFromJson>.toContracts(): List<Contract> {
    return mapNotNull { Contract.fromJsonObject(it) }
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
        fun fromJsonObject(contract: ContractFromJson): Contract? {
            val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: return null
            return Contract(
                networkId = contract.networkId,
                blockchain = blockchain,
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