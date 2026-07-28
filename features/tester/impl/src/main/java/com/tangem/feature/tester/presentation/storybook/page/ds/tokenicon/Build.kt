package com.tangem.feature.tester.presentation.storybook.page.ds.tokenicon

import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenIconStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTokenIconStory>.build(): TangemTokenIconStory {
    return TangemTokenIconStory(
        uiState = TangemTokenIconStory.UiStateVariant.Token,
        size = TangemTokenIcon.Size.X56,
        hasUrl = true,
        isGrayscale = false,
        hasIndicator = false,
        isIndicatorPurple = false,
        hasTopIcon = false,
        onUiStateChange = { uiState -> updateStory { it.copy(uiState = uiState) } },
        onSizeChange = { size -> updateStory { it.copy(size = size) } },
        onUrlToggle = { updateStory { it.copy(hasUrl = !it.hasUrl) } },
        onGrayscaleToggle = { updateStory { it.copy(isGrayscale = !it.isGrayscale) } },
        onIndicatorToggle = { updateStory { it.copy(hasIndicator = !it.hasIndicator) } },
        onIndicatorPurpleToggle = { updateStory { it.copy(isIndicatorPurple = !it.isIndicatorPurple) } },
        onTopIconToggle = { updateStory { it.copy(hasTopIcon = !it.hasTopIcon) } },
    )
}

internal val tangemTokenIconStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTokenIconStory>::build)