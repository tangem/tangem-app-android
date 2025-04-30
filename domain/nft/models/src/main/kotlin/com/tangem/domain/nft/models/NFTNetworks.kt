package com.tangem.domain.nft.models

import com.tangem.domain.tokens.model.Network

data class NFTNetworks(
    val availableNetworks: List<Network>,
    val unavailableNetworks: List<Network>,
)