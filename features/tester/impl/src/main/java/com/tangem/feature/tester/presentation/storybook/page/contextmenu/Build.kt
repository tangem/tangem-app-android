package com.tangem.feature.tester.presentation.storybook.page.contextmenu

import com.tangem.feature.tester.presentation.storybook.entity.TangemContextMenuStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemContextMenuStory>.build(): TangemContextMenuStory {
    return TangemContextMenuStory(
        isExpanded = false,
        onExpandedChange = { expanded ->
            updateStory { it.copy(isExpanded = expanded) }
        },
    )
}

internal val tangemContextMenuStoryFactory
    get() = storyPageFactory(StateUpdater<TangemContextMenuStory>::build)