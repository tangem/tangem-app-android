package com.tangem.blockchainsdk.compatibility

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
import com.tangem.datasource.api.markets.models.response.TokenMarketListResponse
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

val l2BlockchainsList = Blockchain.entries.filter { it.isL2EthereumNetwork() }

val l2BlockchainsCoinIds = l2BlockchainsList.map { it.toCoinId() }

val ETHEREUM_COIN_ID = Blockchain.Ethereum.toCoinId()

fun getL2CompatibilityTokenComparison(token: UserTokensResponse.Token, currencyId: String): Boolean {
    return if (currencyId == ETHEREUM_COIN_ID) {
        l2BlockchainsCoinIds.contains(token.id) || currencyId == token.id
    } else {
        token.id == currencyId
    }
}

fun List<CoinsResponse.Coin.Network>.applyL2Compatibility(coinId: String): List<CoinsResponse.Coin.Network> {
    return if (coinId == ETHEREUM_COIN_ID) {
        val l2Networks = l2BlockchainsList.map { blockchain ->
            CoinsResponse.Coin.Network(
                networkId = blockchain.toNetworkId(),
            )
        }
        this + l2Networks
    } else {
        this
    }
}

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

fun getTokenIdIfL2Network(tokenId: String): String {
    return if (l2BlockchainsCoinIds.contains(tokenId)) {
        ETHEREUM_COIN_ID
    } else {
        tokenId
    }
}