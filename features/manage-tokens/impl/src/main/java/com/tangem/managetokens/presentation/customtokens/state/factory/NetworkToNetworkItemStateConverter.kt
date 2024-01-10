package com.tangem.managetokens.presentation.customtokens.state.factory

import com.tangem.core.ui.extensions.getActiveIconResByNetworkId
import com.tangem.domain.tokens.model.Network
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.utils.converter.Converter

internal class NetworkToNetworkItemStateConverter(
    private val onNetworkItemSelected: (NetworkItemState) -> Unit,
) : Converter<Network, NetworkItemState> {
    override fun convert(value: Network): NetworkItemState {
        return NetworkItemState.Selectable(
            name = value.name,
            protocolName = value.standardType.name,
            iconResId = getActiveIconResByNetworkId(value.backendId),
            id = value.backendId,
            onNetworkClick = onNetworkItemSelected,
        )
    }
}