package com.tangem.tap.domain.tokens

import com.squareup.moshi.JsonClass
import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.common.extensions.fromNetworkId

@JsonClass(generateAdapter = true)
data class CurrencyFromJson(
    val id: String,
    val name: String,
    val symbol: String,
    val networks: List<ContractFromJson>? = null
)

@JsonClass(generateAdapter = true)
data class ContractFromJson(
    val networkId: String,
    val contractAddress: String?,
    val decimalCount: Int?
)

@JsonClass(generateAdapter = true)
data class CurrenciesFromJson(
    val imageHost: String?,
    val coins: List<CurrencyFromJson>
)

fun List<ContractFromJson>.toContracts(): List<Contract> {
    return mapNotNull { Contract.fromJsonObject(it) }
}

data class Currency(
    val id: String,
    val name: String,
    val symbol: String,
    val iconUrl: String,
    val contracts: List<Contract>
) {

    companion object {
        fun fromJsonObject(currency: CurrencyFromJson): Currency {
            return Currency(
                id = currency.id,
                name = currency.name,
                symbol = currency.symbol,
                iconUrl = getIconUrl(currency.id, null),
                contracts = currency.networks?.toContracts() ?: emptyList()
            )
        }

        fun fromCoinResponse(currency: CoinsResponse.Coin, imageHost: String?): Currency {
            return Currency(
                id = currency.id,
                name = currency.name,
                symbol = currency.symbol,
                iconUrl = getIconUrl(currency.id, imageHost),
                contracts = currency.networks.mapNotNull { Contract.fromNetwork(it, imageHost) }
            )
        }
    }
}

data class Contract(
    val networkId: String,
    val blockchain: Blockchain,
    val address: String?,
    val decimalCount: Int?,
    val iconUrl: String,
) {

    companion object {
        fun fromJsonObject(contract: ContractFromJson): Contract? {
            val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: return null
            return Contract(
                networkId = contract.networkId,
                blockchain = blockchain,
                address = contract.contractAddress,
                decimalCount = contract.decimalCount,
                iconUrl = getIconUrl(contract.networkId, null)
            )
        }

        fun fromNetwork(contract: CoinsResponse.Coin.Network, imageHost: String?): Contract? {
            val blockchain = Blockchain.fromNetworkId(contract.networkId) ?: return null
            return Contract(
                networkId = contract.networkId,
                blockchain = blockchain,
                address = contract.contractAddress,
                decimalCount = contract.decimalCount?.toInt(),
                iconUrl = getIconUrl(contract.networkId, imageHost)
            )
        }
    }
}

fun getIconUrl(id: String, imageHost: String? = null): String {
    return "${imageHost ?: DEFAULT_IMAGE_HOST}large/$id.png"
}

private const val DEFAULT_IMAGE_HOST =
    "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/"
