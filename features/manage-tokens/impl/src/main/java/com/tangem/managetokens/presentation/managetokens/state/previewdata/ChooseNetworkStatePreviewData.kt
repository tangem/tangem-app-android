package com.tangem.managetokens.presentation.managetokens.state.previewdata

import androidx.compose.runtime.mutableStateOf
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.ChooseNetworkState
import kotlinx.collections.immutable.toImmutableList

internal object ChooseNetworkStatePreviewData {

    val state = ChooseNetworkState(
        nativeNetworks = nativeNetworks.toImmutableList(),
        nonNativeNetworks = nonNativeNetworks.toImmutableList(),
        onNonNativeNetworkHintClick = {},
        onCloseChooseNetworkScreen = {},
    )
}

internal val nativeNetworks = listOf(
    NetworkItemState.Toggleable(
        name = "Ethereum",
        protocolName = "ETH",
        iconResId = mutableStateOf(R.drawable.img_polygon_22),
        isMainNetwork = true,
        isAdded = mutableStateOf(true),
        id = "",
        onToggleClick = { _, _ -> },
        address = "",
        decimals = 0,
    ),
)

internal val nonNativeNetworks = listOf(
    NetworkItemState.Toggleable(
        name = "Ethereum",
        protocolName = "ETH",
        iconResId = mutableStateOf(R.drawable.img_kusama_22),
        isMainNetwork = false,
        isAdded = mutableStateOf(true),
        id = "",
        onToggleClick = { _, _ -> },
        address = "",
        decimals = 0,
    ),
    NetworkItemState.Toggleable(
        name = "BNB SMART CHAIN",
        protocolName = "BEP20",
        iconResId = mutableStateOf(R.drawable.ic_bsc_16),
        isMainNetwork = false,
        isAdded = mutableStateOf(false),
        id = "",
        onToggleClick = { _, _ -> },
        address = "",
        decimals = 0,
    ),
)