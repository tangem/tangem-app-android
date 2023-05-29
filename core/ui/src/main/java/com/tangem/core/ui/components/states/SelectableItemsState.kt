package com.tangem.core.ui.components.states

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class SelectableItemsState<T>(
    val selectedItem: Item<T>,
    val items: ImmutableList<Item<T>>,
)

data class Item<T>(
    val id: Int,
    val startText: TextReference,
    val endText: TextReference,
    val isSelected: Boolean,
    val data: T,
)