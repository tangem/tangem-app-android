package com.tangem.features.collectibles.impl.addfunds

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** State of the "Add funds" bottom sheet — a list of top-up methods. */
@Immutable
internal data class CollectiblesAddFundsUM(
    val items: ImmutableList<Item>,
    val onDismiss: () -> Unit,
) {

    @Immutable
    internal data class Item(
        val icon: TangemIconUM,
        val title: TextReference,
        val description: TextReference,
        val onClick: () -> Unit,
    )
}