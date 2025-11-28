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
                is DraggableItem.Portfolio,
                is DraggableItem.Placeholder,
                -> DraggableItem.RoundingMode.None
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

internal fun List<DraggableItem>.uniteItemsV2(isAccountsMode: Boolean): List<DraggableItem> {
    val items = this
    val lastItemIndex = items.lastIndex

    return items
        .asSequence()
        .mapIndexed { index, item ->
            val mode = when (index) {
                0 -> if (item is DraggableItem.Placeholder) {
                    DraggableItem.RoundingMode.None
                } else {
                    DraggableItem.RoundingMode.Top()
                }
                lastItemIndex -> DraggableItem.RoundingMode.Bottom()
                1 -> if (items.first() is DraggableItem.Placeholder) {
                    DraggableItem.RoundingMode.Top()
                } else {
                    DraggableItem.RoundingMode.None
                }
                else -> when (item) {
                    is DraggableItem.Placeholder -> DraggableItem.RoundingMode.None
                    is DraggableItem.GroupHeader -> if (isAccountsMode) {
                        DraggableItem.RoundingMode.None
                    } else {
                        DraggableItem.RoundingMode.Top(showGap = true)
                    }
                    is DraggableItem.Token -> applyRoundingModeToToken(
                        isAccountsMode = isAccountsMode,
                        items = items,
                        index = index,
                        lastItemIndex = lastItemIndex,
                    )
                    is DraggableItem.Portfolio -> DraggableItem.RoundingMode.Top(showGap = true)
                }
            }

            item
                .updateRoundingMode(mode)
                .updateShadowVisibility(show = false)
        }.toList()
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

        val itemsWithoutFirstPlaceholder = items.filterNot { it.id == firstPlaceholderId }

        addAll(itemsWithoutFirstPlaceholder)
    }
}

/**
 * Applying rounding to tokens
 *
 * If is in accounts mode without grouping
 * * PORTFOLIO
 * * TOKEN
 * * TOKEN          <- add rounding
 * * PORTFOLIO      index + 1 is PORTFOLIO
 *
 * If is in accounts mode with grouping
 * * PORTFOLIO
 * * PLACEHOLDER
 * * GROUPING
 * * TOKEN
 * * TOKEN          <- add rounding
 * * PLACEHOLDER    index + 1 is PLACEHOLDER
 * * PORTFOLIO      index + 2 is PORTFOLIO
 * * PLACEHOLDER
 *
 * If is not accounts mode without grouping
 * * TOKEN
 * * TOKEN          <- add rounding
 *
 * If is not accounts mode with grouping
 * * PLACEHOLDER
 * * GROUPING
 * * TOKEN
 * * TOKEN          <- add rounding
 * * PLACEHOLDER    index + 1 is PLACEHOLDER
 */
private fun applyRoundingModeToToken(
    isAccountsMode: Boolean,
    items: List<DraggableItem>,
    index: Int,
    lastItemIndex: Int,
) = when {
    isAccountsMode && index + 1 < lastItemIndex &&
        (items[index + 1] is DraggableItem.Portfolio ||
            items[index + 1] is DraggableItem.Placeholder && items[index + 2] is DraggableItem.Portfolio) -> {
        DraggableItem.RoundingMode.Bottom(showGap = true)
    }
    (!isAccountsMode || index + 1 == lastItemIndex) && items[index + 1] is DraggableItem.Placeholder -> {
        DraggableItem.RoundingMode.Bottom(showGap = true)
    }
    else -> DraggableItem.RoundingMode.None
}