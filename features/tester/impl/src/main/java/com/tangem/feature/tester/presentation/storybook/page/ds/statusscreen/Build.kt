package com.tangem.feature.tester.presentation.storybook.page.ds.statusscreen

import com.tangem.core.ui.ds2.statusscreen.TangemStatusScreen
import com.tangem.feature.tester.presentation.storybook.entity.TangemStatusScreenStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemStatusScreenStory>.build(): TangemStatusScreenStory {
    return TangemStatusScreenStory(
        variant = TangemStatusScreen.Variant.Success,
        hasDescription = true,
        hasAsset = true,
        hasExtraBottom = false,
        hasDetails = true,
        hasFooter = true,
        isFullscreen = false,
        replayKey = 0,
        onVariantChange = { variant -> updateStory { it.copy(variant = variant, replayKey = it.replayKey + 1) } },
        onDescriptionToggle = { updateStory { it.copy(hasDescription = !it.hasDescription) } },
        onAssetToggle = { updateStory { it.copy(hasAsset = !it.hasAsset) } },
        onExtraBottomToggle = { updateStory { it.copy(hasExtraBottom = !it.hasExtraBottom) } },
        onDetailsToggle = { updateStory { it.copy(hasDetails = !it.hasDetails) } },
        onFooterToggle = { updateStory { it.copy(hasFooter = !it.hasFooter) } },
        onFullscreenToggle = {
            updateStory { it.copy(isFullscreen = !it.isFullscreen, replayKey = it.replayKey + 1) }
        },
        onReplayClick = { updateStory { it.copy(replayKey = it.replayKey + 1) } },
    )
}

internal val tangemStatusScreenStoryFactory
    get() = storyPageFactory(StateUpdater<TangemStatusScreenStory>::build)