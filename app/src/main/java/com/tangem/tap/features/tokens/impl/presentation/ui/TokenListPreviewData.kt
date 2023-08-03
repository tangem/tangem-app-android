package com.tangem.tap.features.tokens.impl.presentation.ui

import androidx.compose.runtime.mutableStateOf
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.tokens.impl.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import com.tangem.wallet.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
* [REDACTED_AUTHOR]
 */
object TokenListPreviewData {

    fun createManageToken(): TokenItemState.ManageContent {
        return TokenItemState.ManageContent(
            fullName = "Tether (USDT)",
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
            networks = createManageNetworksList(),
            composedId = "11231",
            id = "",
            name = "Tether",
            symbol = "",
        )
    }

    fun createReadToken(): TokenItemState.ReadContent {
        return TokenItemState.ReadContent(
            id = "1",
            fullName = "Tether (USDT)",
            iconUrl = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/tether.png",
            networks = createReadNetworksList(),
            composedId = "11231",
        )
    }

    fun createManageNetworksList(): ImmutableList<NetworkItemState.ManageContent> {
        return persistentListOf(
            NetworkItemState.ManageContent(
                name = "Ethereum",
                protocolName = "MAIN",
                iconResId = mutableStateOf(R.drawable.ic_eth_no_color),
                isMainNetwork = true,
                isAdded = mutableStateOf(true),
                id = "",
                address = null,
                onToggleClick = { _, _ -> },
                onNetworkClick = {},
                decimalCount = null,
                blockchain = Blockchain.Ethereum,
            ),
            NetworkItemState.ManageContent(
                name = "BNB SMART CHAIN",
                protocolName = "BEP20",
                iconResId = mutableStateOf(R.drawable.ic_bsc_no_color),
                isMainNetwork = false,
                isAdded = mutableStateOf(false),
                id = "",
                address = null,
                onToggleClick = { _, _ -> },
                onNetworkClick = {},
                decimalCount = null,
                blockchain = Blockchain.BSC,
            ),
        )
    }

    fun createReadNetworksList(): ImmutableList<NetworkItemState.ReadContent> {
        return persistentListOf(
            NetworkItemState.ReadContent(
                name = "Ethereum",
                protocolName = "MAIN",
                iconResId = mutableStateOf(R.drawable.ic_eth_no_color),
                isMainNetwork = true,
            ),
            NetworkItemState.ReadContent(
                name = "BNB SMART CHAIN",
                protocolName = "BEP20",
                iconResId = mutableStateOf(R.drawable.ic_bsc_no_color),
                isMainNetwork = false,
            ),
        )
    }
}
