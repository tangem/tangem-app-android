package com.tangem.data.common.currency

import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.models.network.Network

fun getBlockchain(networkId: Network.ID): Blockchain {
    return Blockchain.fromId(networkId.rawId.value)
}