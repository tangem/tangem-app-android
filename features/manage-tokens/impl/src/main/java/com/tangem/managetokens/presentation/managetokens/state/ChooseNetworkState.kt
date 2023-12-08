package com.tangem.managetokens.presentation.managetokens.state

import com.tangem.managetokens.presentation.common.state.NetworkItemState
import kotlinx.collections.immutable.ImmutableList

internal data class ChooseNetworkState(
    val nativeNetworks: ImmutableList<NetworkItemState>,
    val nonNativeNetworks: ImmutableList<NetworkItemState>,
    val onNonNativeNetworkHintClick: () -> Unit,
    val onCloseChooseNetworkScreen: () -> Unit,
)
