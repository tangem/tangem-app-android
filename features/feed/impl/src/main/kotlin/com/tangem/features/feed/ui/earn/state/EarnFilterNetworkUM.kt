package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable

@Immutable
internal sealed class EarnFilterNetworkUM {

    abstract val isSelected: Boolean

    data class AllNetworks(
        override val isSelected: Boolean,
    ) : EarnFilterNetworkUM()

    data class MyNetworks(
        override val isSelected: Boolean,
    ) : EarnFilterNetworkUM()

    data class Network(
        override val isSelected: Boolean,
        val id: String,
        val text: String,
        val symbol: String,
        val iconRes: Int,
    ) : EarnFilterNetworkUM()
}