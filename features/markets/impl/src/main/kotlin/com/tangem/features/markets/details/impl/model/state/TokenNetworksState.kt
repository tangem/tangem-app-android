package com.tangem.features.markets.details.impl.model.state

import com.tangem.domain.markets.TokenMarketInfo

internal sealed class TokenNetworksState {

    data object Loading : TokenNetworksState()

    data object NoNetworksAvailable : TokenNetworksState()

    data class NetworksAvailable(val networks: List<TokenMarketInfo.Network>) : TokenNetworksState()
}