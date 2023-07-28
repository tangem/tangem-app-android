package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem.RoundingMode

/**
 * Update item [RoundingMode]
 *
 * @param mode new [RoundingMode]
 *
 * @return updated [DraggableItem]
 * */
internal fun DraggableItem.updateRoundingMode(mode: RoundingMode): DraggableItem = when (this) {
    is DraggableItem.GroupPlaceholder -> this
    is DraggableItem.GroupHeader -> this.copy(roundingMode = mode)
    is DraggableItem.Token -> this.copy(roundingMode = mode)
}

/**
 * Update item shadow visibility
 *
 * @param show if true then item should be elevated
 *
 * @return updated [DraggableItem]
 * */
internal fun DraggableItem.updateShadowVisibility(show: Boolean): DraggableItem = when (this) {
    is DraggableItem.GroupPlaceholder -> this
    is DraggableItem.GroupHeader -> this.copy(showShadow = show)
    is DraggableItem.Token -> this.copy(showShadow = show)
}

internal fun getGroupPlaceholder(index: Int): DraggableItem.GroupPlaceholder {
    return DraggableItem.GroupPlaceholder(id = "placeholder_${index.inc()}")
}
