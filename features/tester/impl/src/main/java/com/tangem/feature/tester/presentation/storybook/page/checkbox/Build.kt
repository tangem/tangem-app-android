@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.checkbox

import com.tangem.feature.tester.presentation.storybook.entity.TangemCheckboxStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemCheckboxStory>.build(): TangemCheckboxStory {
    return TangemCheckboxStory(
        isRoundedChecked = false,
        onRoundedCheckedChange = { checked ->
            updateStory { it.copy(isRoundedChecked = checked) }
        },
        isCircleChecked = false,
        onCircleCheckedChange = { checked ->
            updateStory { it.copy(isCircleChecked = checked) }
        },
    )
}

internal val tangemCheckboxStoryFactory
    get() = storyPageFactory(StateUpdater<TangemCheckboxStory>::build)