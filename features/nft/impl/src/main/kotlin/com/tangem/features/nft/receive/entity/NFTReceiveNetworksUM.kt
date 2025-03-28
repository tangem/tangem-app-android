package com.tangem.features.nft.receive.entity

import com.tangem.core.ui.components.fields.entity.SearchBarUM
import kotlinx.collections.immutable.ImmutableList

internal data class NFTReceiveNetworksUM(
    val searchBar: SearchBarUM,
    val networks: ImmutableList<NFTNetworkUM>,
)