package com.tangem.blockchainsdk.compatibility

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
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
        val l2Networks = l2BlockchainsList.map {
            CoinsResponse.Coin.Network(
                networkId = it.toNetworkId(),
            )
        }
        this + l2Networks
    } else {
        this
    }
}

fun TokenMarketInfoResponse.applyL2Compatibility(coinId: String): TokenMarketInfoResponse {
    val networks = this.networks ?: return this
    return if (coinId == ETHEREUM_COIN_ID) {
        val l2Networks = l2BlockchainsList.map {
            TokenMarketInfoResponse.Network(
                networkId = it.toNetworkId(),
                contractAddress = null,
                decimalCount = null,
            )
        }
        this.copy(networks = networks + l2Networks)
    } else {
        this
    }
}

fun getTokenIdIfL2Network(tokenId: String): String {
    return if (l2BlockchainsCoinIds.contains(tokenId)) {
        ETHEREUM_COIN_ID
    } else {
        tokenId
    }
}