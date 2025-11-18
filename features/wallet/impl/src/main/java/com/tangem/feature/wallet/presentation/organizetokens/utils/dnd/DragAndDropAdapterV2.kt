package com.tangem.feature.wallet.presentation.organizetokens.utils.dnd

import com.tangem.feature.wallet.presentation.organizetokens.DragAndDropIntents
import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.model.OrganizeTokensListUM
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.divideMovingItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItemsV2
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.updateItems
import com.tangem.utils.Provider
import kotlinx.collections.immutable.mutate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import org.burnoutcrew.reorderable.ItemPosition

internal class DragAndDropAdapterV2(
    private val tokenListUMProvider: Provider<OrganizeTokensListUM>,
) : DragAndDropIntents {

    private val tokenListUM: OrganizeTokensListUM
        get() = tokenListUMProvider.invoke()

    private val draggableGroupsOperations = DraggableGroupsOperations()

    private val dragAndDropUpdatesInternal: MutableStateFlow<DragOperation?> = MutableStateFlow(value = null)

    private var draggingItem: DraggableItem? = null
    private var draggingListState: OrganizeTokensListUM? = null

    val dragAndDropUpdates: Flow<DragOperation>
        get() = dragAndDropUpdatesInternal.filterNotNull()

    override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean {
        val items = when (val listState = tokenListUM) {
            is OrganizeTokensListUM.AccountList -> listState.items
            is OrganizeTokensListUM.TokensList -> {
                if (tokenListUM.isGrouped) {
                    listState.items
                } else {
                    return true // If ungrouped then item can be moved anywhere
                }
            }
            OrganizeTokensListUM.EmptyList -> return true
        }

        val (dragOverItem, draggingItem) = findItemsToMove(
            items = items,
            moveOverItemKey = dragOver.key,
            movedItemKey = dragging.key,
        )

        if (dragOverItem == null || draggingItem == null) {
            return false
        }

        val canDrag = when (draggingItem) {
            is DraggableItem.GroupHeader -> checkCanMoveHeaderOver(
                item = draggingItem,
                moveOverItem = dragOverItem,
            )
            is DraggableItem.Token -> checkCanMoveTokenOver(
                item = draggingItem,
                moveOverItem = dragOverItem,
                isAccountsMode = tokenListUM is OrganizeTokensListUM.AccountList,
                isGrouped = tokenListUM.isGrouped,
            )
            is DraggableItem.Placeholder,
            is DraggableItem.Portfolio,
            -> false
        }

        return canDrag
    }

    override fun onItemDraggingStart(item: DraggableItem) {
        if (draggingItem != null) return
        draggingItem = item

        updateListState(DragOperation.Type.Start) {
            when (item) {
                is DraggableItem.Placeholder,
                is DraggableItem.Portfolio,
                -> items
                is DraggableItem.GroupHeader -> draggableGroupsOperations.collapseGroupV2(items, item)
                    .divideMovingItem(item)
                is DraggableItem.Token -> items.divideMovingItem(item)
            }
        }

        draggingListState = tokenListUM
    }

    override fun onItemDraggingEnd() {
        val draggingItem = draggingItem ?: return

        updateListState(DragOperation.Type.End(isItemsOrderChanged = checkIsItemsOrderChanged())) {
            when (draggingItem) {
                is DraggableItem.GroupHeader -> {
                    draggableGroupsOperations.expandGroupsV2(items)
                        .uniteItemsV2(tokenListUM is OrganizeTokensListUM.AccountList)
                }
                is DraggableItem.Token -> {
                    items.uniteItemsV2(tokenListUM is OrganizeTokensListUM.AccountList)
                }
                is DraggableItem.Placeholder,
                is DraggableItem.Portfolio,
                -> items
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

    private fun updateListState(type: DragOperation.Type, block: OrganizeTokensListUM.() -> List<DraggableItem>) {
        val updatedState = tokenListUM.updateItems { block(tokenListUM) }

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

    private fun checkCanMoveHeaderOver(item: DraggableItem.GroupHeader, moveOverItem: DraggableItem): Boolean {
        return when (moveOverItem) {
            // Header can be moved only in its account
            is DraggableItem.Placeholder -> item.accountId == moveOverItem.accountId
            else -> false
        }
    }

    private fun checkCanMoveTokenOver(
        item: DraggableItem.Token,
        moveOverItem: DraggableItem,
        isGrouped: Boolean,
        isAccountsMode: Boolean,
    ): Boolean {
        return when (moveOverItem) {
            is DraggableItem.GroupHeader -> false // Token item can not be moved to group item
            is DraggableItem.Token -> when {
                // Token item can be moved only in its group
                isGrouped -> item.groupId == moveOverItem.groupId

                // Token item can be moved only in its account
                isAccountsMode -> item.accountId == moveOverItem.accountId

                // If ungrouped and not accounts mode then item can be moved anywhere
                else -> true
            }
            is DraggableItem.Portfolio,
            is DraggableItem.Placeholder,
            -> false // Token item can not be moved to portfolio or placeholder
        }
    }

    private fun checkIsItemsOrderChanged(): Boolean {
        fun OrganizeTokensListUM?.getItemsIds(): List<Any>? = this?.items?.mapNotNull { item ->
            if (item is DraggableItem.Placeholder) {
                null
            } else {
                item.id
            }
        }

        return tokenListUM.getItemsIds() != draggingListState.getItemsIds()
    }

    data class DragOperation(
        val type: Type,
        val listState: OrganizeTokensListUM,
    ) {

        sealed class Type {

            data object Start : Type()

            data object Dragged : Type()

            data class End(val isItemsOrderChanged: Boolean) : Type()
        }
    }
}