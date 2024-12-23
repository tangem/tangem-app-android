package com.tangem.feature.wallet.presentation.organizetokens.utils.dnd

import com.tangem.feature.wallet.presentation.organizetokens.DragAndDropIntents
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.divideMovingItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.updateItems
import com.tangem.utils.Provider
import kotlinx.collections.immutable.mutate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.burnoutcrew.reorderable.ItemPosition

internal class DragAndDropAdapter(
    private val listStateProvider: Provider<OrganizeTokensListState>,
) : DragAndDropIntents {

    private val draggableGroupsOperations = DraggableGroupsOperations()

    private val externalListState: OrganizeTokensListState
        get() = listStateProvider.invoke()

    private val dragAndDropUpdatesInternal: MutableStateFlow<DragOperation?> = MutableStateFlow(value = null)

    private var draggingItem: DraggableItem? = null
    private var draggingListState: OrganizeTokensListState? = null

    val dragAndDropUpdates: Flow<DragOperation>
        get() = dragAndDropUpdatesInternal.filterNotNull()

    override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean {
        val items = when (val listState = externalListState) {
            is OrganizeTokensListState.GroupedByNetwork -> listState.items
            is OrganizeTokensListState.Empty,
            is OrganizeTokensListState.Ungrouped,
            -> return true // If ungrouped then item can be moved anywhere
        }

        val (dragOverItem, draggingItem) = findItemsToMove(
            items = items,
            moveOverItemKey = dragOver.key,
            movedItemKey = dragging.key,
        )

        if (dragOverItem == null || draggingItem == null) {
            return false
        }

        return when (draggingItem) {
            is DraggableItem.GroupHeader -> checkCanMoveHeaderOver(dragOver, dragOverItem, items.lastIndex)
            is DraggableItem.Token -> checkCanMoveTokenOver(draggingItem, dragOverItem)
            is DraggableItem.Placeholder -> false
        }
    }

    override fun onItemDraggingStart(item: DraggableItem) {
        if (draggingItem != null) return
        draggingItem = item

        updateListState(DragOperation.Type.Start) {
            when (item) {
                is DraggableItem.Placeholder -> items
                is DraggableItem.GroupHeader -> draggableGroupsOperations.collapseGroup(items, item)
                is DraggableItem.Token -> when (this) {
                    is OrganizeTokensListState.GroupedByNetwork -> items.divideMovingItem(item)
                    is OrganizeTokensListState.Ungrouped -> items.divideMovingItem(item)
                    is OrganizeTokensListState.Empty -> items
                }
            }
        }

        draggingListState = externalListState
    }

    override fun onItemDraggingEnd() {
        val draggingItem = draggingItem ?: return

        updateListState(DragOperation.Type.End(isItemsOrderChanged = checkIsItemsOrderChanged())) {
            when (draggingItem) {
                is DraggableItem.GroupHeader -> draggableGroupsOperations.expandGroups(items)
                is DraggableItem.Token -> items.uniteItems()
                is DraggableItem.Placeholder -> items
            }
        }

        this.draggingItem = null
    }

    override fun onItemDragged(from: ItemPosition, to: ItemPosition) {
        updateListState(DragOperation.Type.Dragged) {
            items.mutate {
                it.add(to.index, it.removeAt(from.index))
            }
        }
    }

    private fun updateListState(type: DragOperation.Type, block: OrganizeTokensListState.() -> List<DraggableItem>) {
        val updatedState = externalListState.updateItems { block(externalListState) }

        dragAndDropUpdatesInternal.value = DragOperation(type, updatedState)
    }

    private fun findItemsToMove(
        items: List<DraggableItem>,
        moveOverItemKey: Any?,
        movedItemKey: Any?,
    ): Pair<DraggableItem?, DraggableItem?> {
        var moveOverItem: DraggableItem? = null
        var movedItem: DraggableItem? = null

        for (item in items) {
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

    private fun checkCanMoveHeaderOver(
        moveOverItemPosition: ItemPosition,
        moveOverItem: DraggableItem,
        lastItemIndex: Int,
    ): Boolean {
        // Group item can be moved only to group divider or to ages of the items list
        return when {
            moveOverItemPosition.index == 0 -> true
            moveOverItemPosition.index == lastItemIndex -> true
            moveOverItem is DraggableItem.Placeholder -> true
            else -> false
        }
    }

    private fun checkCanMoveTokenOver(item: DraggableItem.Token, moveOverItem: DraggableItem): Boolean {
        // Token item can be moved only in its group
        return when (moveOverItem) {
            is DraggableItem.GroupHeader -> false // Token item can not be moved to group item
            is DraggableItem.Token -> item.groupId == moveOverItem.groupId // Token item can not be moved over its group
            is DraggableItem.Placeholder -> false
        }
    }

    private fun checkIsItemsOrderChanged(): Boolean {
        fun OrganizeTokensListState?.getItemsIds(): List<Any>? = this?.items?.mapNotNull { item ->
            if (item is DraggableItem.Placeholder) {
                null
            } else {
                item.id
            }
        }

        return externalListState.getItemsIds() != draggingListState.getItemsIds()
    }

    data class DragOperation(
        val type: Type,
        val listState: OrganizeTokensListState,
    ) {

        sealed class Type {

            data object Start : Type()

            data object Dragged : Type()

            data class End(val isItemsOrderChanged: Boolean) : Type()
        }
    }
}