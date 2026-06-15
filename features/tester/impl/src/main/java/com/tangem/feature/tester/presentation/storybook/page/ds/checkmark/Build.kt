package com.tangem.feature.tester.presentation.storybook.page.ds.checkmark

import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckmarkStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemCheckmarkStory>.build(): TangemCheckmarkStory {
    return TangemCheckmarkStory(
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

internal val tangemCheckmarkStoryFactory
    get() = storyPageFactory(StateUpdater<TangemCheckmarkStory>::build)