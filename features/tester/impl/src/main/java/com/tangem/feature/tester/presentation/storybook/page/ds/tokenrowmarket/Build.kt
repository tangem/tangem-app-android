package com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowmarket

import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowMarketStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTokenRowMarketStory>.build(): TangemTokenRowMarketStory {
    return TangemTokenRowMarketStory(
        direction = TangemPriceChange.Direction.Up,
        isShimmer = false,
        hasTicker = true,
        hasPosition = true,
        hasCapitalization = true,
        hasPrice = true,
        hasPriceChange = true,
        hasChart = true,
        longTitle = false,
        onDirectionChange = { direction -> updateStory { it.copy(direction = direction) } },
        onShimmerToggle = { updateStory { it.copy(isShimmer = !it.isShimmer) } },
        onTickerToggle = { updateStory { it.copy(hasTicker = !it.hasTicker) } },
        onPositionToggle = { updateStory { it.copy(hasPosition = !it.hasPosition) } },
        onCapitalizationToggle = { updateStory { it.copy(hasCapitalization = !it.hasCapitalization) } },
        onPriceToggle = { updateStory { it.copy(hasPrice = !it.hasPrice) } },
        onPriceChangeToggle = { updateStory { it.copy(hasPriceChange = !it.hasPriceChange) } },
        onChartToggle = { updateStory { it.copy(hasChart = !it.hasChart) } },
        onLongTitleToggle = { updateStory { it.copy(longTitle = !it.longTitle) } },
    )
}

internal val tangemTokenRowMarketStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTokenRowMarketStory>::build)