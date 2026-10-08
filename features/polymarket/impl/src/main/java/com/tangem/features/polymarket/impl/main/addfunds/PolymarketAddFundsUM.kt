package com.tangem.features.polymarket.impl.main.addfunds

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class PolymarketAddFundsUM(
    val items: ImmutableList<Item>,
    val onDismiss: () -> Unit,
) {

    @Immutable
    internal data class Item(
        val icon: ImageVector,
        val title: TextReference,
        val description: TextReference,
        val onClick: () -> Unit,
    )
}