package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem

internal fun List<DraggableItem>.uniteItems(): List<DraggableItem> {
    val lastItemIndex = this.lastIndex

    return this.mapIndexed { index, item ->
        val mode = when (index) {
            0 -> DraggableItem.RoundingMode.Top()
            lastItemIndex -> DraggableItem.RoundingMode.Bottom()
            else -> when (item) {
                is DraggableItem.GroupHeader -> DraggableItem.RoundingMode.Top(showGap = true)
                is DraggableItem.Token -> if (this[index + 1] is DraggableItem.GroupPlaceholder) {
                    DraggableItem.RoundingMode.Bottom(showGap = true)
                } else {
                    DraggableItem.RoundingMode.None
                }
                is DraggableItem.GroupPlaceholder -> DraggableItem.RoundingMode.None
            }
        }

        item
            .updateRoundingMode(mode)
            .updateShadowVisibility(show = false)
    }
}