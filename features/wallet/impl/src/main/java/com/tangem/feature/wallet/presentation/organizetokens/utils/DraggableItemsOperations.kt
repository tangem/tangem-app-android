package com.tangem.feature.wallet.presentation.organizetokens.utils

import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem
import org.burnoutcrew.reorderable.ItemPosition

internal fun List<DraggableItem>.findItemsToMove(
    moveOverItemKey: Any?,
    movedItemKey: Any?,
): Pair<DraggableItem?, DraggableItem?> {
    var moveOverItem: DraggableItem? = null
    var movedItem: DraggableItem? = null

    for (item in this) {
        if (item.id == moveOverItemKey) {
            moveOverItem = item
        }
        if (item.id == movedItemKey) {
            movedItem = item
        }
        if (moveOverItem != null && movedItem != null) {
            break
        }
    }

    return Pair(moveOverItem, movedItem)
}

internal fun checkCanMoveHeaderOver(
    moveOverItemPosition: ItemPosition,
    moveOverItem: DraggableItem,
    lastItemIndex: Int,
): Boolean {
    return when {
        moveOverItemPosition.index == 0 -> true
        moveOverItemPosition.index == lastItemIndex -> true
        moveOverItem is DraggableItem.GroupDivider -> true
        else -> false
    }
}

internal fun checkCanMoveTokenOver(item: DraggableItem.Token, moveOverItem: DraggableItem): Boolean {
    return when (moveOverItem) {
        is DraggableItem.GroupHeader -> false // Token item can not be moved to group item
        is DraggableItem.Token -> item.groupId == moveOverItem.groupId // Token item can not be moved over its group
        is DraggableItem.GroupDivider -> false
    }
}

internal fun List<DraggableItem>.moveItem(fromIndex: Int, toIndex: Int): List<DraggableItem> {
    return this.toMutableList()
        .apply { add(toIndex, removeAt(fromIndex)) }
}
