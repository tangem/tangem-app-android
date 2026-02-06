package com.tangem.features.feed.ui.earn.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

@Immutable
internal sealed class EarnFilterNetworkUM {

    abstract val text: TextReference
    abstract val isSelected: Boolean

    data class AllNetworks(
        override val text: TextReference,
        override val isSelected: Boolean,
    ) : EarnFilterNetworkUM()

    data class MyNetworks(
        override val text: TextReference,
        override val isSelected: Boolean,
    ) : EarnFilterNetworkUM()

    data class Network(
        val id: String,
        override val text: TextReference,
        val symbol: TextReference,
        val iconUrl: String?,
        override val isSelected: Boolean,
    ) : EarnFilterNetworkUM()
}