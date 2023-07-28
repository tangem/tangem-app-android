package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.DraggableItem
import kotlinx.collections.immutable.PersistentList
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
    // Group item can be moved only to group divider or to ages of the items list
    return when {
        moveOverItemPosition.index == 0 -> true
        moveOverItemPosition.index == lastItemIndex -> true
        moveOverItem is DraggableItem.GroupPlaceholder -> true
        else -> false
    }
}

internal fun checkCanMoveTokenOver(item: DraggableItem.Token, moveOverItem: DraggableItem): Boolean {
    // Token item can be moved only in its group
    return when (moveOverItem) {
        is DraggableItem.GroupHeader -> false // Token item can not be moved to group item
        is DraggableItem.Token -> item.groupId == moveOverItem.groupId // Token item can not be moved over its group
        is DraggableItem.GroupPlaceholder -> false
    }
}

internal fun PersistentList<DraggableItem>.moveItem(fromIndex: Int, toIndex: Int): PersistentList<DraggableItem> {
    val fromItem = this[fromIndex]
    return this
        .removeAt(fromIndex)
        .add(toIndex, fromItem)
}

internal fun List<DraggableItem>.divideItems(movingItem: DraggableItem): List<DraggableItem> {
    return this.map {
        it
            .updateRoundingMode(DraggableItem.RoundingMode.All(showGap = true))
            .updateShadowVisibility(show = it.id == movingItem.id)
    }
}

@Suppress("UNCHECKED_CAST") // Erased type
internal fun <T : DraggableItem> List<T>.uniteItems(): List<T> {
    val lastItemIndex = this.lastIndex

    return this.mapIndexed { index, item ->
        val mode = when (index) {
            0 -> DraggableItem.RoundingMode.Top()
            lastItemIndex -> DraggableItem.RoundingMode.Bottom()
            else -> DraggableItem.RoundingMode.None
        }

        item
            .updateRoundingMode(mode)
            .updateShadowVisibility(show = false)
    } as List<T>
}
// [REDACTED_TODO_COMMENT]
@Volatile
private var groupIdToTokens: Map<String, List<DraggableItem.Token>>? = null

internal fun List<DraggableItem>.collapseGroup(group: DraggableItem.GroupHeader): List<DraggableItem> {
    if (!groupIdToTokens.isNullOrEmpty()) return this

    groupIdToTokens = this
        .asSequence()
        .filterIsInstance<DraggableItem.Token>()
        .groupBy { it.groupId }

    return this
        .filterNot { it is DraggableItem.Token && it.groupId == group.id }
        .divideGroups(group)
}

internal fun List<DraggableItem>.expandGroups(): List<DraggableItem> {
    if (groupIdToTokens.isNullOrEmpty()) return this

    val currentGroups = this.filterIsInstance<DraggableItem.GroupHeader>()
    val lastGroupIndex = currentGroups.lastIndex

    return currentGroups
        .flatMapIndexed { index, group ->
            buildList {
                add(group)
                addAll(groupIdToTokens?.get(group.id).orEmpty())
                if (index != lastGroupIndex) {
                    add(DraggableItem.GroupPlaceholder(id = "group_divider_$index"))
                }
            }
        }
        .uniteItems()
        .also { groupIdToTokens = null }
}

/**
 * Applies the correct [DraggableItem.RoundingMode] and shadow status to each item in the list,
 * based on the relationship of each item to the [movingItem] and its position in the list.
 *
 * @param movingItem The item that is being dragged/moved.
 * @return A list of [DraggableItem]s with updated rounding modes and shadow statuses.
 */
internal fun List<DraggableItem>.divideGroups(movingItem: DraggableItem): List<DraggableItem> {
    val lastItemIndex = this.lastIndex

    return this.mapIndexed { index, item ->
        when {
            // Case when current item is the moving item
            item.id == movingItem.id -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.All(showGap = true))
                    .updateShadowVisibility(show = true)
            }
            // Case when moving item is a token and current item is the group of the moving token
            movingItem is DraggableItem.Token && item.id == movingItem.groupId -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.All(showGap = true))
                    .updateShadowVisibility(show = true)
            }
            // Case when both moving item and current item are tokens and belong to the same group
            movingItem is DraggableItem.Token &&
                item is DraggableItem.Token && item.groupId == movingItem.groupId -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.All(showGap = true))
                    .updateShadowVisibility(show = false)
            }
            // Case when current item is the first item in the list
            index == 0 -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.Top())
                    .updateShadowVisibility(show = false)
            }
            // Case when current item is the last item in the list
            index == lastItemIndex -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.Bottom())
                    .updateShadowVisibility(show = false)
            }
            // Case when previous item is a GroupPlaceholder
            this[index - 1] is DraggableItem.GroupPlaceholder -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.Top(showGap = true))
                    .updateShadowVisibility(show = false)
            }
            // Case when next item is a GroupPlaceholder
            this[index + 1] is DraggableItem.GroupPlaceholder -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.Bottom(showGap = true))
                    .updateShadowVisibility(show = false)
            }
            // Default case when none of the above conditions are met
            else -> {
                item
                    .updateRoundingMode(DraggableItem.RoundingMode.None)
                    .updateShadowVisibility(show = false)
            }
        }
    }
}
