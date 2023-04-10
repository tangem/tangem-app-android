package com.tangem.tap.features.tokens.presentation.ui

import com.tangem.tap.features.tokens.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.presentation.states.TokenItemState
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
[REDACTED_AUTHOR]
 */
object TokenListPreviewData {

    fun createManageToken(): TokenItemState.ManageAccess {
        return TokenItemState.ManageAccess(
            name = "Tether (USDT)",
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
            networks = createManageNetworksList(),
            id = "",
        )
    }

    fun createReadToken(): TokenItemState.ReadAccess {
        return TokenItemState.ReadAccess(
            name = "Tether (USDT)",
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
            networks = createReadNetworksList(),
        )
    }

    fun createManageNetworksList(): ImmutableList<NetworkItemState.ManageAccess> {
        return persistentListOf(
            NetworkItemState.ManageAccess(
                name = "Ethereum",
                protocolName = "MAIN",
                iconResId = R.drawable.ic_eth_no_color,
                isMainNetwork = true,
                isAdded = true,
                networkId = "",
                contractAddress = null,
                onToggleClick = { _, _ -> },
                onNetworkClick = {},
            ),
            NetworkItemState.ManageAccess(
                name = "BNB SMART CHAIN",
                protocolName = "BEP20",
                iconResId = R.drawable.ic_bsc_no_color,
                isMainNetwork = false,
                isAdded = false,
                networkId = "",
                contractAddress = null,
                onToggleClick = { _, _ -> },
                onNetworkClick = {},
            ),
        )
    }

    fun createReadNetworksList(): ImmutableList<NetworkItemState.ReadAccess> {
        return persistentListOf(
            NetworkItemState.ReadAccess(
                name = "Ethereum",
                protocolName = "MAIN",
                iconResId = R.drawable.ic_eth_no_color,
                isMainNetwork = true,
            ),
            NetworkItemState.ReadAccess(
                name = "BNB SMART CHAIN",
                protocolName = "BEP20",
                iconResId = R.drawable.ic_bsc_no_color,
                isMainNetwork = false,
            ),
        )
    }
}