package com.tangem.feature.wallet.presentation.organizetokens.utils.dnd

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.divideMovingItem
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.getGroupPlaceholder
import com.tangem.feature.wallet.presentation.organizetokens.utils.common.uniteItems

internal class DraggableGroupsOperations {

    private var groupIdToTokens: Map<Int, List<DraggableItem.Token>>? = null

    fun collapseGroup(items: List<DraggableItem>, movingGroup: DraggableItem.GroupHeader): List<DraggableItem> {
        if (!groupIdToTokens.isNullOrEmpty()) return items

        groupIdToTokens = items
            .asSequence()
            .filterIsInstance<DraggableItem.Token>()
            .groupBy { it.groupId }

        val itemsWithoutGroupTokens = items.filterNot {
            it is DraggableItem.Token && it.groupId == movingGroup.id
        }

        return itemsWithoutGroupTokens.divideMovingItem(movingGroup)
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
}