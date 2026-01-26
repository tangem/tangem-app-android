package com.tangem.features.feed.model.market.details.state

import com.tangem.domain.markets.TokenMarketInfo

internal sealed class TokenNetworksState {

    data object Loading : TokenNetworksState()

    data object NoNetworksAvailable : TokenNetworksState()

    data class NetworksAvailable(val networks: List<TokenMarketInfo.Network>) : TokenNetworksState()
}