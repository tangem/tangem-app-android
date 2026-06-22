package com.tangem.feature.tester.presentation.storybook.page.ds.topnavigation

import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.feature.tester.presentation.storybook.entity.TangemTopNavigationStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTopNavigationStory>.build(): TangemTopNavigationStory {
    return TangemTopNavigationStory(
        contentAlign = TangemTopNavigation.ContentAlign.Start,
        background = TangemTopNavigationStory.Background.Rainbow,
        contentMode = TangemTopNavigationStory.ContentMode.Plain,
        hasBack = true,
        hasSubtitle = true,
        longTitle = false,
        endButton = TangemTopNavigationStory.EndButton.Close,
        endGroup = TangemTopNavigationStory.EndGroup.None,
        useStatusBarInsets = true,
        isFadeEnabled = true,
        isBlurEnabled = true,
        onContentAlignChange = { align ->
            updateStory { it.copy(contentAlign = align) }
        },
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onContentModeChange = { mode ->
            updateStory { it.copy(contentMode = mode) }
        },
        onBackToggle = {
            updateStory { it.copy(hasBack = !it.hasBack) }
        },
        onSubtitleToggle = {
            updateStory { it.copy(hasSubtitle = !it.hasSubtitle) }
        },
        onLongTitleToggle = {
            updateStory { it.copy(longTitle = !it.longTitle) }
        },
        onEndButtonChange = { endButton ->
            updateStory { it.copy(endButton = endButton) }
        },
        onEndGroupChange = { endGroup ->
            updateStory { it.copy(endGroup = endGroup) }
        },
        onStatusBarInsetsToggle = {
            updateStory { it.copy(useStatusBarInsets = !it.useStatusBarInsets) }
        },
        onFadeToggle = {
            updateStory { it.copy(isFadeEnabled = !it.isFadeEnabled) }
        },
        onBlurToggle = {
            updateStory { it.copy(isBlurEnabled = !it.isBlurEnabled) }
        },
    )
}

internal val tangemTopNavigationStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTopNavigationStory>::build)