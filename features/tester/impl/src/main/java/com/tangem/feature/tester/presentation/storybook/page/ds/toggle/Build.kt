package com.tangem.feature.tester.presentation.storybook.page.ds.toggle

import com.tangem.feature.tester.presentation.storybook.entity.TangemSwitchStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemSwitchStory>.build(): TangemSwitchStory {
    return TangemSwitchStory(
        isChecked = false,
        isEnabled = true,
        onCheckedChange = { checked ->
            updateStory { it.copy(isChecked = checked) }
        },
        onEnabledToggle = {
            updateStory { it.copy(isEnabled = !it.isEnabled) }
        },
    )
}

internal val tangemSwitchStoryFactory
    get() = storyPageFactory(StateUpdater<TangemSwitchStory>::build)