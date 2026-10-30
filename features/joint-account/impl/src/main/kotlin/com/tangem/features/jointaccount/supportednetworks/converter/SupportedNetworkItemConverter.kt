package com.tangem.features.jointaccount.supportednetworks.converter

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.domain.models.network.Network
import com.tangem.features.jointaccount.supportednetworks.ui.state.JointSupportedNetworksUM.NetworkItemUM
import com.tangem.utils.converter.Converter

/** `null` for a network id the blockchain SDK does not know */
internal object SupportedNetworkItemConverter : Converter<Network.RawID, NetworkItemUM?> {

    private val blockchainByNetworkId: Map<String, Blockchain> by lazy {
        Blockchain.entries.associateBy { it.toNetworkId() }
    }

    override fun convert(value: Network.RawID): NetworkItemUM? {
        val blockchain = blockchainByNetworkId[value.value] ?: return null

        return NetworkItemUM(
            id = value.value,
            name = blockchain.fullName,
            symbol = blockchain.currency,
            icon = TangemIconUM.Image(imageRes = getActiveIconRes(blockchain)),
        )
    }
}