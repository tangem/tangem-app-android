@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.tokenrow

import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemTokenRowStory>.build(): TangemTokenRowStory {
    return TangemTokenRowStory(
        isBalanceHidden = false,
        onBalanceHiddenToggle = { updateStory { it.copy(isBalanceHidden = !it.isBalanceHidden) } },
    )
}

internal val tangemTokenRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemTokenRowStory>::build)