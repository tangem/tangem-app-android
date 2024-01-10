package com.tangem.managetokens.presentation.customtokens.state.previewdata

import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.customtokens.state.ChooseNetworkState
import kotlinx.collections.immutable.persistentListOf

internal object ChooseNetworkCustomPreviewData {
    val networks = persistentListOf(
        NetworkItemState.Selectable(
            name = "Ethereum",
            protocolName = "ETH",
            iconResId = R.drawable.img_kusama_22,
            id = "ethereum",
            onNetworkClick = { },
        ),
        NetworkItemState.Selectable(
            name = "BNB SMART CHAIN",
            protocolName = "BEP20",
            iconResId = R.drawable.ic_bsc_16,
            id = "binance smart chain",
            onNetworkClick = { },
        ),
    )
    val state = ChooseNetworkState(
        networks,
        selectedNetwork = networks.first(),
        onCloseChoosingNetworkClick = {},
        onChooseNetworkClick = {},
        show = true,
    )
}