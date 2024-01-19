package com.tangem.managetokens.presentation.managetokens.state.factory

import com.tangem.domain.tokens.model.Token
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.ChooseNetworkState
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class NetworksToChooseNetworkStateConverter(
    private val networkToNetworkItemStateConverter: NetworkToNetworkItemStateConverter,
    private val onNonNativeNetworkHintClick: () -> Unit,
    private val onCloseChooseNetworkScreen: () -> Unit,
) : Converter<List<Token.Network>, ChooseNetworkState> {

    override fun convert(value: List<Token.Network>): ChooseNetworkState {
        return createChooseNetworksState(value)
    }

    private fun createChooseNetworksState(networks: List<Token.Network>): ChooseNetworkState {
        val nativeNetworks = mutableListOf<NetworkItemState>()
        val nonNativeNetworks = mutableListOf<NetworkItemState>()
        networks.map { networkToNetworkItemStateConverter.convert(it) }.forEach {
            if (it is NetworkItemState.Toggleable && it.isMainNetwork) {
                nativeNetworks.add(it)
            } else {
                nonNativeNetworks.add(it)
            }
        }
        return ChooseNetworkState(
            nativeNetworks = nativeNetworks.toPersistentList(),
            nonNativeNetworks = nonNativeNetworks.toPersistentList(),
            onNonNativeNetworkHintClick = onNonNativeNetworkHintClick,
            onCloseChooseNetworkScreen = onCloseChooseNetworkScreen,
        )
    }
}