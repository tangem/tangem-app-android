package com.tangem.feature.wallet.child.organizetokens.model.common

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem

internal fun List<DraggableItem>.uniteItems(isAccountsMode: Boolean): List<DraggableItem> {
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