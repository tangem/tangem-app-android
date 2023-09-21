package com.tangem.feature.wallet.presentation.organizetokens.utils.dnd

import com.tangem.common.Provider
import com.tangem.feature.wallet.presentation.organizetokens.DragAndDropIntents
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListState
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.divideMovingItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.updateItems
import kotlinx.collections.immutable.mutate
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.burnoutcrew.reorderable.ItemPosition

internal class DragAndDropAdapter(
    private val listStateProvider: Provider<OrganizeTokensListState>,
) : DragAndDropIntents {

    private val draggableGroupsOperations = DraggableGroupsOperations()

    private val currentListState: OrganizeTokensListState
        get() = listStateProvider.invoke()

    private val listStateFlowInternal: MutableSharedFlow<OrganizeTokensListState> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private var currentDraggingItem: DraggableItem? = null

    val stateFlow: Flow<OrganizeTokensListState>
        get() = listStateFlowInternal

    override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean {
        val items = when (val listState = currentListState) {
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
        if (currentDraggingItem != null) return
        currentDraggingItem = item

        updateListState {
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
    }

    override fun onItemDraggingEnd() {
        val draggingItem = currentDraggingItem ?: return

        updateListState {
            when (draggingItem) {
                is DraggableItem.GroupHeader -> draggableGroupsOperations.expandGroups(items)
                is DraggableItem.Token -> items.uniteItems()
                is DraggableItem.Placeholder -> items
            }
        }

        currentDraggingItem = null
    }

    override fun onItemDragged(from: ItemPosition, to: ItemPosition) = updateListState {
        items.mutate {
            it.add(to.index, it.removeAt(from.index))
        }
    }

    private fun updateListState(block: OrganizeTokensListState.() -> List<DraggableItem>) {
        val updatedState = currentListState.updateItems { block(currentListState) }

        listStateFlowInternal.tryEmit(updatedState)
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
}