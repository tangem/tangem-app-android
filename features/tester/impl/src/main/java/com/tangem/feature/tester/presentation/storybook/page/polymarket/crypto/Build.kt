package com.tangem.feature.tester.presentation.storybook.page.polymarket.crypto

import com.tangem.feature.tester.presentation.storybook.entity.PolymarketCryptoChartStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal val cryptoChartStoryFactory
    get() = storyPageFactory(StateUpdater<PolymarketCryptoChartStory>::build)

private fun StateUpdater<PolymarketCryptoChartStory>.build() = PolymarketCryptoChartStory(
    isCompact = false,
    isLive = false,
    updateMode = PolymarketCryptoChartStory.UpdateMode.Regular,
    sample = PolymarketCryptoChartStory.Sample.Bitcoin,
    interval = PolymarketCryptoChartStory.Interval.Minute,
    target = PolymarketCryptoChartStory.Target.Inside,
    isDark = true,
    fontScale = 1f,
    onLive = { value -> updateStory { it.copy(isLive = value) } },
    onUpdateMode = { value -> updateStory { it.copy(updateMode = value) } },
    onCompact = { value -> updateStory { it.copy(isCompact = value, isLive = false) } },
    onSample = { value -> updateStory { it.copy(sample = value) } },
    onInterval = { value -> updateStory { it.copy(interval = value) } },
    onTarget = { value -> updateStory { it.copy(target = value) } },
    onDark = { value -> updateStory { it.copy(isDark = value) } },
    onFontScale = { value -> updateStory { it.copy(fontScale = value) } },
)