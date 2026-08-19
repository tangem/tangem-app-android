package com.tangem.data.markets

import com.tangem.blockchainsdk.compatibility.ETHEREUM_COIN_ID
import com.tangem.blockchainsdk.compatibility.l2BlockchainsList
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.store.datasource.markets.models.response.TokenMarketInfoResponse
import com.tangem.store.datasource.markets.models.response.TokenMarketListResponse

fun TokenMarketInfoResponse.applyL2Compatibility(coinId: String): TokenMarketInfoResponse {
    val networks = this.networks ?: return this
    if (coinId != ETHEREUM_COIN_ID) return this

    val networksWithL2 = networks.appendMissingL2Networks(
        networkId = { it.networkId },
        createNetwork = { networkId ->
            TokenMarketInfoResponse.Network(
                networkId = networkId,
                contractAddress = null,
                decimalCount = null,
            )
        },
    )
    return this.copy(networks = networksWithL2)
}

fun TokenMarketListResponse.Token.applyL2Compatibility(): TokenMarketListResponse.Token {
    val networks = this.networks ?: return this
    if (id != ETHEREUM_COIN_ID) return this

    val networksWithL2 = networks.appendMissingL2Networks(
        networkId = { it.networkId },
        createNetwork = { networkId ->
            TokenMarketListResponse.Token.Network(
                networkId = networkId,
                contractAddress = null,
                decimalCount = null,
            )
        },
    )
    return this.copy(networks = networksWithL2)
}

private inline fun <T> List<T>.appendMissingL2Networks(
    networkId: (T) -> String,
    createNetwork: (networkId: String) -> T,
): List<T> {
    val existingNetworkIds = mapTo(hashSetOf(), networkId)
    val missingL2Networks = l2BlockchainsList
        .map { it.toNetworkId() }
        .filterNot { it in existingNetworkIds }
        .map(createNetwork)
    return this + missingL2Networks
}