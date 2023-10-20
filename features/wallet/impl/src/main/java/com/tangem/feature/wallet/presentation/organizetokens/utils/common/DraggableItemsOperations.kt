package com.tangem.feature.wallet.presentation.organizetokens.utils.common

import com.tangem.feature.wallet.presentation.organizetokens.model.DraggableItem

internal fun List<DraggableItem>.uniteItems(): List<DraggableItem> {
    val items = prepareItems()
    val lastItemIndex = items.lastIndex

    return prepareItems().mapIndexed { index, item ->
        val mode = when (index) {
            // 1 index is used because the first item is always a placeholder, check `prepareItems()` function
            1 -> DraggableItem.RoundingMode.Top()
            lastItemIndex -> DraggableItem.RoundingMode.Bottom()
            else -> when (item) {
                is DraggableItem.Placeholder -> DraggableItem.RoundingMode.None
                is DraggableItem.GroupHeader -> DraggableItem.RoundingMode.Top(showGap = true)
                is DraggableItem.Token -> if (items[index + 1] is DraggableItem.Placeholder) {
                    DraggableItem.RoundingMode.Bottom(showGap = true)
                } else {
                    DraggableItem.RoundingMode.None
                }
            }
        }

        item
            .updateRoundingMode(mode)
            .updateShadowVisibility(show = false)
    }
}

internal fun List<DraggableItem>.divideMovingItem(movingItem: DraggableItem): List<DraggableItem> {
    val mutableList = this.toMutableList()
    val listIterator = mutableList.listIterator()

    while (listIterator.hasNext()) {
        val item = listIterator.next()

        if (item.id == movingItem.id) {
            val dividedItem = movingItem
                .updateRoundingMode(DraggableItem.RoundingMode.All())
                .updateShadowVisibility(show = true)

            listIterator.set(dividedItem)
            break
        }
    }

    return mutableList
}

/**
 * !!! Workaround !!!
 *
 * We need to add a [DraggableItem.Placeholder] (since it's not draggable) as the first item of the list, because the
 * [DND library](https://github.com/aclassen/ComposeReorderable) glitches when a user tries to drag the first item.
 *
 * @since 07.09.2023
 * */
private fun List<DraggableItem>.prepareItems(): List<DraggableItem> {
    val firstPlaceholderId = "initial_placeholder"
    val items = this

    return mutableListOf<DraggableItem>().apply {
        add(DraggableItem.Placeholder(firstPlaceholderId))

        val itemsWithoutFirstPlaceholder = if (items.firstOrNull()?.id == firstPlaceholderId) {
            items.drop(n = 1)
        } else {
            items
        }

        addAll(itemsWithoutFirstPlaceholder)
    }
}