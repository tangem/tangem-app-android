@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.tab

import com.tangem.feature.tester.presentation.storybook.entity.TangemTabStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTabStory>.build(): TangemTabStory {
    return TangemTabStory(
        checkedIndex = 0,
        onCheckedIndexChange = { index ->
            updateStory { it.copy(checkedIndex = index) }
        },
    )
}

internal val tangemTabStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTabStory>::build)