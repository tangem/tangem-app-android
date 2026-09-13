package com.tangem.features.feed.earn.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference

/** A row of the "Filter by network" sheet: the scope-wide "All" option, or a single network. */
@Immutable
internal sealed interface EarnFilterNetworkUM {

    val isSelected: Boolean
    val onClick: () -> Unit

    data class All(
        override val isSelected: Boolean,
        override val onClick: () -> Unit,
    ) : EarnFilterNetworkUM

    data class Network(
        override val isSelected: Boolean,
        override val onClick: () -> Unit,
        val id: String,
        val name: TextReference,
        val symbol: String,
        val iconRes: Int,
    ) : EarnFilterNetworkUM
}