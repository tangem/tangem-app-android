package com.tangem.domain.nft.models

import com.tangem.domain.models.network.Network

data class NFTNetworks(
    val availableNetworks: List<Network>,
    val unavailableNetworks: List<Network>,
)