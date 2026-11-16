package com.tangem.features.polymarket.impl.main.addfunds.network

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketChooseNetworkUM(
    val networks: ImmutableList<Network>,
    val onDismiss: () -> Unit,
) {

    @Immutable
    internal data class Network(
        val id: String,
        val icon: TangemTokenIcon.State,
        val name: TextReference,
        val symbol: TextReference,
        val onClick: () -> Unit,
    )
}