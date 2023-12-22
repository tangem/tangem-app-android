package com.tangem.managetokens.presentation.customtokens.state

import com.tangem.managetokens.presentation.common.state.NetworkItemState
import kotlinx.collections.immutable.ImmutableList

internal data class ChooseNetworkState(
    val networks: ImmutableList<NetworkItemState>,
    val selectedNetwork: NetworkItemState?,
    val onChooseNetworkClick: () -> Unit,
    val onCloseChoosingNetworkClick: () -> Unit,
    val show: Boolean = false,
)