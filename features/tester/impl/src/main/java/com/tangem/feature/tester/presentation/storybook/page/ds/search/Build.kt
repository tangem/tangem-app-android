package com.tangem.feature.tester.presentation.storybook.page.ds.search

import com.tangem.feature.tester.presentation.storybook.entity.TangemSearchStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemSearchStory>.build(): TangemSearchStory {
    return TangemSearchStory(
        background = TangemSearchStory.Background.BgSecondary,
        placeholder = TangemSearchStory.Placeholder.Short,
        hasCloseButton = true,
        onBackgroundChange = { background ->
            updateStory { it.copy(background = background) }
        },
        onPlaceholderChange = { placeholder ->
            updateStory { it.copy(placeholder = placeholder) }
        },
        onCloseButtonToggle = {
            updateStory { it.copy(hasCloseButton = !it.hasCloseButton) }
        },
    )
}

internal val tangemSearchStoryFactory
    get() = storyPageFactory(StateUpdater<TangemSearchStory>::build)