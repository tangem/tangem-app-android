package com.tangem.domain.marketing.models

sealed interface MarketingCampaignTarget {

    /**
     * token_details / staking / yield campaigns target a network + contract address.
     * [contractAddress] is `null` for native coins (the backend omits it), so a `null`/blank contract
     * matches the coin of that network.
     */
    data class NetworkContract(val networkId: String, val contractAddress: String?) : MarketingCampaignTarget

    /** markets_token campaigns target a CoinGecko token id. */
    data class CoingeckoId(val id: String) : MarketingCampaignTarget
}