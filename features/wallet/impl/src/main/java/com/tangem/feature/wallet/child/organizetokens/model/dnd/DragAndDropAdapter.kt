package com.tangem.feature.wallet.child.organizetokens.model.dnd

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeRowItemUM
import com.tangem.feature.wallet.child.organizetokens.entity.OrganizeTokensUM
import com.tangem.feature.wallet.child.organizetokens.entity.RoundingModeUM
import com.tangem.feature.wallet.child.organizetokens.model.DragAndDropIntents
import com.tangem.feature.wallet.child.organizetokens.model.common.uniteItems
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.burnoutcrew.reorderable.ItemPosition

internal class DragAndDropAdapter(
    private val organizeTokensUMFlow: StateFlow<OrganizeTokensUM>,
) : DragAndDropIntents {

    private var draggingItem: OrganizeRowItemUM? = null
    private var draggingListState: OrganizeTokensUM? = null

    private val tokensUM: OrganizeTokensUM
        get() = organizeTokensUMFlow.value

    private val draggableGroupsOperations by lazy(LazyThreadSafetyMode.NONE) {
        DraggableGroupsOperations()
    }

    val dragAndDropUpdates: StateFlow<DragOperation?>
        field = MutableStateFlow(value = null)

    override fun canDragItemOver(dragOver: ItemPosition, dragging: ItemPosition): Boolean {
        val tokensListUM = tokensUM.tokenList

        val (dragOverItem, draggingItem) = findItemsToMove(
            items = tokensListUM,
            moveOverItemKey = dragOver.key,
            movedItemKey = dragging.key,
        )

        if (dragOverItem == null || draggingItem == null) {
            return false
        }

        val canDrag = when (draggingItem) {
            is OrganizeRowItemUM.Network -> checkCanMoveHeaderOver(
                item = draggingItem,
                moveOverItem = dragOverItem,
            )
            is OrganizeRowItemUM.Token -> checkCanMoveTokenOver(
                item = draggingItem,
                moveOverItem = dragOverItem,
                isAccountsMode = tokensUM.isAccountsMode,
                isGrouped = tokensUM.isGrouped,
            )
            is OrganizeRowItemUM.Placeholder,
            is OrganizeRowItemUM.Portfolio,
            -> false
        }

        return canDrag
    }

    override fun onItemDraggingStartLegacy(item: DraggableItem) {
        /* no-op */
    }

    override fun onItemDraggingStart(item: OrganizeRowItemUM) {
        if (draggingItem != null) return
        draggingItem = item

        dragAndDropUpdates.value = DragOperation(
            type = DragOperation.Type.Start,
            tokenList = when (item) {
                is OrganizeRowItemUM.Placeholder,
                is OrganizeRowItemUM.Portfolio,
                -> tokensUM.tokenList
                is OrganizeRowItemUM.Network -> draggableGroupsOperations
                    .collapseGroup(tokensUM.tokenList, item)
                    .divideMovingItem(item)
                is OrganizeRowItemUM.Token -> tokensUM.tokenList.divideMovingItem(item)
            }.toPersistentList(),
        )

        draggingListState = tokensUM
    }

    override fun onItemDraggingEnd() {
        val draggingItem = draggingItem ?: return

        dragAndDropUpdates.value = DragOperation(
            type = DragOperation.Type.End(isItemsOrderChanged = checkIsItemsOrderChanged(tokensUM)),
            tokenList = when (draggingItem) {
                is OrganizeRowItemUM.Network -> draggableGroupsOperations
                    .expandGroups(tokensUM.tokenList, tokensUM.isAccountsMode)
                    .uniteItems(tokensUM.isAccountsMode)
                is OrganizeRowItemUM.Token -> tokensUM.tokenList.uniteItems(tokensUM.isAccountsMode)
                is OrganizeRowItemUM.Placeholder,
                is OrganizeRowItemUM.Portfolio,
                -> tokensUM.tokenList
            }.toPersistentList(),
        )

        this.draggingItem = null
    }

    override fun onItemDragged(from: ItemPosition, to: ItemPosition) {
        dragAndDropUpdates.value = DragOperation(
            type = DragOperation.Type.Dragged,
            tokenList = tokensUM.tokenList.mutate {
                it.add(to.index, it.removeAt(from.index))
            }.toPersistentList(),
        )
    }

    private fun findItemsToMove(
        items: List<OrganizeRowItemUM>,
        moveOverItemKey: Any?,
        movedItemKey: Any?,
    ): Pair<OrganizeRowItemUM?, OrganizeRowItemUM?> {
        var moveOverItem: OrganizeRowItemUM? = null
        var movedItem: OrganizeRowItemUM? = null

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

    private fun checkCanMoveHeaderOver(item: OrganizeRowItemUM.Network, moveOverItem: OrganizeRowItemUM) =
        when (moveOverItem) {
            // Header can be moved only in its account
            is OrganizeRowItemUM.Placeholder -> item.accountId == moveOverItem.accountId
            else -> false
        }

    private fun checkCanMoveTokenOver(
        item: OrganizeRowItemUM.Token,
        moveOverItem: OrganizeRowItemUM,
        isGrouped: Boolean,
        isAccountsMode: Boolean,
    ): Boolean {
        return when (moveOverItem) {
            is OrganizeRowItemUM.Network -> false // Token item can not be moved to group item
            is OrganizeRowItemUM.Token -> when {
                // Token item can be moved only in its group
                isGrouped -> item.groupId == moveOverItem.groupId

                // Token item can be moved only in its account
                isAccountsMode -> item.accountId == moveOverItem.accountId

                // If ungrouped and not accounts mode then item can be moved anywhere
                else -> true
            }
            is OrganizeRowItemUM.Portfolio,
            is OrganizeRowItemUM.Placeholder,
            -> false // Token item can not be moved to portfolio or placeholder
        }
    }

    private fun checkIsItemsOrderChanged(tokensUM: OrganizeTokensUM): Boolean {
        fun OrganizeTokensUM?.getItemsIds(): List<Any>? = this?.tokenList?.mapNotNull { item ->
            if (item is OrganizeRowItemUM.Placeholder) {
                null
            } else {
                item.id
            }
        }

        return tokensUM.getItemsIds() != draggingListState.getItemsIds()
    }

    private fun List<OrganizeRowItemUM>.divideMovingItem(movingItem: OrganizeRowItemUM): List<OrganizeRowItemUM> {
        val mutableList = this.toMutableList()
        val listIterator = mutableList.listIterator()

        while (listIterator.hasNext()) {
            val item = listIterator.next()

            if (item.id == movingItem.id) {
                val dividedItem = movingItem
                    .updateRoundingMode(RoundingModeUM.All())
                    .updateShadowVisibility(show = true)

                listIterator.set(dividedItem)
                break
            }
        }

        return mutableList
    }

    data class DragOperation(
        val type: Type,
        val tokenList: PersistentList<OrganizeRowItemUM>,
    ) {

        sealed class Type {

            data object Start : Type()

            data object Dragged : Type()

            data class End(val isItemsOrderChanged: Boolean) : Type()
        }
    }
}