package com.tangem.feature.wallet.child.organizetokens.model

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import org.burnoutcrew.reorderable.ItemPosition

internal interface OrganizeTokensIntents {

    fun onBackClick()

    fun onSortClick()

    fun onGroupClick()

    fun onApplyClick()

    fun onCancelClick()
}

internal interface DragAndDropIntents {

    fun onItemDragged(from: ItemPosition, to: ItemPosition)

    fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean

    fun onItemDraggingStart(item: DraggableItem)

    fun onItemDraggingEnd()
}