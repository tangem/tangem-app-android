package com.tangem.features.jointaccount.supportednetworks.converter

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.domain.models.network.Network
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM.NetworkItemUM

internal object SupportedNetworkItemConverter {

    fun convert(rawIds: List<Network.RawID>): List<NetworkItemUM> {
        val blockchainByNetworkId = Blockchain.entries.associateBy { it.toNetworkId() }
        return rawIds.mapNotNull { rawId ->
            val blockchain = blockchainByNetworkId[rawId.value] ?: return@mapNotNull null
            NetworkItemUM(
                id = rawId.value,
                name = blockchain.fullName,
                symbol = blockchain.currency,
                iconResId = getActiveIconRes(blockchain),
            )
        }
    }
}