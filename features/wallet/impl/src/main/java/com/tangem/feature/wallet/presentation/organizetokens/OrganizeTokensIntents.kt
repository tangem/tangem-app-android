package com.tangem.feature.wallet.presentation.organizetokens

import org.burnoutcrew.reorderable.ItemPosition

internal interface OrganizeTokensIntents {

    fun onBackClick()

    fun onSortClick()

    fun onGroupClick()

    fun onApplyClick()

    fun onCancelClick()

    fun onItemDragged(from: ItemPosition, to: ItemPosition)

    fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean

    fun onItemDraggingStart(item: DraggableItem)

    fun onItemDraggingEnd()
}
