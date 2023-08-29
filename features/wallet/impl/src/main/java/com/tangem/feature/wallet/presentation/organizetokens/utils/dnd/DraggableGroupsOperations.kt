package com.tangem.feature.wallet.presentation.organizetokens.utils.dnd

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupPlaceholder
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems

internal class DraggableGroupsOperations {

    private var groupIdToTokens: Map<String, List<DraggableItem.Token>>? = null

    fun collapseGroup(items: List<DraggableItem>, movingGroup: DraggableItem.GroupHeader): List<DraggableItem> {
        if (!groupIdToTokens.isNullOrEmpty()) return items

        groupIdToTokens = items
            .asSequence()
            .filterIsInstance<DraggableItem.Token>()
            .groupBy { it.groupId }

        val itemsWithoutGroupTokens = items.filterNot {
            it is DraggableItem.Token && it.groupId == movingGroup.id
        }

        return divideGroups(itemsWithoutGroupTokens, movingGroup)
    }

    fun expandGroups(items: List<DraggableItem>): List<DraggableItem> {
        if (groupIdToTokens.isNullOrEmpty()) return items

        val currentGroups = items.filterIsInstance<DraggableItem.GroupHeader>()
        val lastGroupIndex = currentGroups.lastIndex

        val expandedGroups = currentGroups
            .flatMapIndexed { index, group ->
                buildList {
                    add(group)
                    addAll(groupIdToTokens?.get(group.id).orEmpty())
                    if (index != lastGroupIndex) {
                        add(getGroupPlaceholder(index))
                    }
                }
            }
            .uniteItems()

        groupIdToTokens = null

        return expandedGroups
    }

    fun divideGroups(items: List<DraggableItem>, movingItem: DraggableItem): List<DraggableItem> {
        val lastItemIndex = items.lastIndex

        return items.mapIndexed { index, item ->
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
                items[index - 1] is DraggableItem.GroupPlaceholder -> {
                    item
                        .updateRoundingMode(DraggableItem.RoundingMode.Top(showGap = true))
                        .updateShadowVisibility(show = false)
                }
                // Case when next item is a GroupPlaceholder
                items[index + 1] is DraggableItem.GroupPlaceholder -> {
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
}