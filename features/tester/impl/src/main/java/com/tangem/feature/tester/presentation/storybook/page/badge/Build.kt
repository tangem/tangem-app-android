@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.badge

import com.tangem.core.ui.ds.badge.TangemBadgeColor
import com.tangem.feature.tester.presentation.storybook.entity.TangemBadgeStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemBadgeStory>.build(): TangemBadgeStory {
    return TangemBadgeStory(
        selectedColor = TangemBadgeColor.Blue,
        onColorChange = { color ->
            updateStory { it.copy(selectedColor = color) }
        },
    )
}

internal val tangemBadgeStoryFactory
    get() = storyPageFactory(StateUpdater<TangemBadgeStory>::build)