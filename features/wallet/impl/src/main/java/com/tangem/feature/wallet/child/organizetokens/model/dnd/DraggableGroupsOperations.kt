package com.tangem.feature.wallet.child.organizetokens.model.dnd

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.model.common.divideMovingItem
import com.tangem.feature.wallet.child.organizetokens.model.common.getGroupPlaceholderLegacy

internal class DraggableGroupsOperations {

    private var groupIdToTokensLegacy: Map<Int, List<DraggableItem.Token>>? = null

    fun collapseGroupLegacy(items: List<DraggableItem>, movingGroup: DraggableItem.GroupHeader): List<DraggableItem> {
        if (!groupIdToTokensLegacy.isNullOrEmpty()) return items

        groupIdToTokensLegacy = items
            .asSequence()
            .filterIsInstance<DraggableItem.Token>()
            .groupBy { it.groupId }

        val itemsWithoutGroupTokens = items.filterNot {
            it is DraggableItem.Token && it.groupId == movingGroup.id
        }

        return itemsWithoutGroupTokens.divideMovingItem(movingGroup)
    }

    fun expandGroupsLegacy(items: List<DraggableItem>): List<DraggableItem> {
        if (groupIdToTokensLegacy.isNullOrEmpty()) return items

        val accountList = items.filterIsInstance<DraggableItem.Portfolio>()
        val currentGroups = items.filterIsInstance<DraggableItem.GroupHeader>()

        val expandedGroups = if (items.any { it is DraggableItem.Portfolio }) {
            accountList
                .asSequence()
                .flatMap { account ->
                    buildList {
                        add(account)
                        currentGroups
                            .asSequence()
                            .filter { it.accountId == account.id }
                            .forEachIndexed { index, group ->
                                if (index == 0) {
                                    add(getGroupPlaceholderLegacy(accountId = group.accountId, index = -1))
                                }
                                add(group)
                                addAll(groupIdToTokensLegacy?.get(group.id).orEmpty())
                                add(getGroupPlaceholderLegacy(accountId = group.accountId, index = index))
                            }
                    }
                }
        } else {
            currentGroups
                .asSequence()
                .flatMapIndexed { index, group ->
                    buildList {
                        if (index == 0) {
                            add(getGroupPlaceholderLegacy(accountId = group.accountId, index = -1))
                        }
                        add(group)
                        addAll(groupIdToTokensLegacy?.get(group.id).orEmpty())
                        add(getGroupPlaceholderLegacy(accountId = group.accountId, index = index))
                    }
                }
        }.toList()

        groupIdToTokensLegacy = null

        return expandedGroups
    }
}