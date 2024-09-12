package com.tangem.blockchainsdk.compatibility

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.markets.models.response.TokenMarketInfoResponse
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse

val l2NetworksList = listOf(
    Blockchain.Optimism,
    Blockchain.Arbitrum,
    Blockchain.ZkSyncEra,
    Blockchain.Manta,
    Blockchain.PolygonZkEVM,
    Blockchain.Aurora,
    Blockchain.Base,
    Blockchain.Blast,
    Blockchain.Cyber,
)

fun getL2CompatibilityTokenComparison(token: UserTokensResponse.Token, currencyId: String): Boolean {
    return if (currencyId == ETHEREUM_COIN_ID) {
        l2NetworksList.map { it.toCoinId() }.contains(token.id) || currencyId == token.id
    } else {
        token.id == currencyId
    }
}

fun List<CoinsResponse.Coin.Network>.applyL2Compatibility(coinId: String): List<CoinsResponse.Coin.Network> {
    return if (coinId == ETHEREUM_COIN_ID) {
        val l2Networks = l2NetworksList.map {
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
        val l2Networks = l2NetworksList.map {
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

const val ETHEREUM_COIN_ID = "ethereum"
