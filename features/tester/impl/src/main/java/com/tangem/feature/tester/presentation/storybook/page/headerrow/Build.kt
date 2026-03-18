@file:Suppress("MagicNumber", "LongMethod")

package com.tangem.feature.tester.presentation.storybook.page.headerrow

import com.tangem.feature.tester.presentation.storybook.entity.TangemHeaderRowStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemHeaderRowStory>.build(): TangemHeaderRowStory {
    return TangemHeaderRowStory(
        isBalanceHidden = false,
        onBalanceHiddenToggle = { updateStory { it.copy(isBalanceHidden = !it.isBalanceHidden) } },
    )
}

internal val tangemHeaderRowStoryFactory
    get() = storyPageFactory(StateUpdater<TangemHeaderRowStory>::build)