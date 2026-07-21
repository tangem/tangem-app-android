package com.tangem.features.commonfeatures.impl.portfolioselector.converter

import com.tangem.features.commonfeatures.impl.portfolioselector.entity.GroupPosition
import com.tangem.features.commonfeatures.impl.portfolioselector.entity.PortfolioSelectorItemUM
import com.tangem.utils.converter.Converter

/**
 * Assigns each item its [GroupPosition] within a wallet group. Items are grouped by [GroupTitle]
 * boundaries (a title starts a new group), preserving the original order.
 */
internal class PortfolioSelectorGroupPositionsConverter :
    Converter<List<PortfolioSelectorItemUM>, List<PortfolioSelectorItemUM>> {

    override fun convert(value: List<PortfolioSelectorItemUM>): List<PortfolioSelectorItemUM> =
        value.groupByWallet().flatMap { group ->
            group.mapIndexed { index, item ->
                item.withPosition(
                    GroupPosition(
                        indexInGroup = index,
                        lastIndexInGroup = group.lastIndex,
                        isGroupStart = index == 0,
                    ),
                )
            }
        }

    private fun List<PortfolioSelectorItemUM>.groupByWallet(): List<List<PortfolioSelectorItemUM>> =
        fold(mutableListOf<MutableList<PortfolioSelectorItemUM>>()) { groups, item ->
            if (item is PortfolioSelectorItemUM.GroupTitle || groups.isEmpty()) {
                groups.add(mutableListOf(item))
            } else {
                groups.last().add(item)
            }

            groups
        }

    private fun PortfolioSelectorItemUM.withPosition(position: GroupPosition): PortfolioSelectorItemUM = when (this) {
        is PortfolioSelectorItemUM.GroupTitle -> copy(groupPosition = position)
        is PortfolioSelectorItemUM.Portfolio -> copy(groupPosition = position)
    }
}