package com.tangem.feature.wallet.child.organizetokens.model

import androidx.compose.runtime.Stable
import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import org.burnoutcrew.reorderable.ItemPosition

internal interface OrganizeTokensIntents {

    fun onBackClick()

    fun onSortClick()

    fun onGroupClick()

    fun onApplyClick()

    fun onCancelClick()
}

@Stable
internal interface DragAndDropIntents {

    fun onItemDragged(from: ItemPosition, to: ItemPosition)

    fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean

    fun onItemDraggingStartLegacy(item: DraggableItem)
    fun onItemDraggingStart(item: OrganizeRowItemUM)

    fun onItemDraggingEnd()
}