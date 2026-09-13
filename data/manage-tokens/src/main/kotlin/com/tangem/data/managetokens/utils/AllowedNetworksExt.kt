package com.tangem.data.managetokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.network.Network

internal fun Blockchain.isAllowedIn(allowedNetworkIds: Set<Network.RawID>?): Boolean {
    return allowedNetworkIds == null || Network.RawID(toNetworkId()) in allowedNetworkIds
}