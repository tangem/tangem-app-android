package com.tangem.feature.wallet.child.organizetokens.model.common

import com.tangem.feature.wallet.child.organizetokens.entity.DraggableItem
import com.tangem.feature.wallet.child.organizetokens.entity.RoundingModeUM

internal fun List<DraggableItem>.uniteItemsLegacy(isAccountsMode: Boolean): List<DraggableItem> {
    val items = this
    val lastItemIndex = items.lastIndex

    return items
        .asSequence()
        .mapIndexed { index, item ->
            val mode = when (index) {
                0 -> if (item is DraggableItem.Placeholder) {
                    RoundingModeUM.None
                } else {
                    RoundingModeUM.Top()
                }
                lastItemIndex -> RoundingModeUM.Bottom()
                1 -> if (items.first() is DraggableItem.Placeholder) {
                    RoundingModeUM.Top()
                } else {
                    RoundingModeUM.None
                }
                else -> when (item) {
                    is DraggableItem.Placeholder -> RoundingModeUM.None
                    is DraggableItem.GroupHeader -> if (isAccountsMode) {
                        RoundingModeUM.None
                    } else {
                        RoundingModeUM.Top(isShowGap = true)
                    }
                    is DraggableItem.Token -> applyRoundingModeToTokenLegacy(
                        isAccountsMode = isAccountsMode,
                        items = items,
                        index = index,
                        lastItemIndex = lastItemIndex,
                    )
                    is DraggableItem.Portfolio -> RoundingModeUM.Top(isShowGap = true)
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
                .updateRoundingMode(RoundingModeUM.All())
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
private fun applyRoundingModeToTokenLegacy(
    isAccountsMode: Boolean,
    items: List<DraggableItem>,
    index: Int,
    lastItemIndex: Int,
) = when {
    isAccountsMode && index + 1 < lastItemIndex &&
        (items[index + 1] is DraggableItem.Portfolio ||
            items[index + 1] is DraggableItem.Placeholder && items[index + 2] is DraggableItem.Portfolio) -> {
        RoundingModeUM.Bottom(isShowGap = true)
    }
    (!isAccountsMode || index + 1 == lastItemIndex) && items[index + 1] is DraggableItem.Placeholder -> {
        RoundingModeUM.Bottom(isShowGap = true)
    }
    else -> RoundingModeUM.None
}