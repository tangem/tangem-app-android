package com.tangem.domain.marketing.models

sealed interface MarketingCampaignTarget {

    /** token_details / staking / yield campaigns target a network + contract address. */
    data class NetworkContract(val networkId: String, val contractAddress: String) : MarketingCampaignTarget

    /** token_markets campaigns target a CoinGecko token id. */
    data class CoingeckoId(val id: String) : MarketingCampaignTarget
}