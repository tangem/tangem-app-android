package com.tangem.features.addressbook.selectnetworks.state.transformers.converter

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.common.ui.extensions.getActiveIconRes
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM.NetworkItemUM
import com.tangem.utils.converter.Converter

internal class SelectNetworkItemConverter : Converter<SelectNetworkItemConverter.Input, NetworkItemUM> {

    data class Input(
        val blockchain: Blockchain,
        val isSelected: Boolean,
        val onToggle: (networkId: String) -> Unit,
    )

    override fun convert(value: Input): NetworkItemUM {
        val id = value.blockchain.toNetworkId()
        return NetworkItemUM(
            id = id,
            name = value.blockchain.fullName,
            symbol = value.blockchain.currency,
            iconResId = getActiveIconRes(value.blockchain),
            isSelected = value.isSelected,
            onCheckedChange = { value.onToggle(id) },
        )
    }
}