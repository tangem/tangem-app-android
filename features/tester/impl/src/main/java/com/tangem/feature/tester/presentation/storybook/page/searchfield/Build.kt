package com.tangem.feature.tester.presentation.storybook.page.searchfield

import com.tangem.core.ui.ds.field.search.TangemFieldShape
import com.tangem.feature.tester.presentation.storybook.entity.TangemSearchFieldStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemSearchFieldStory>.build(): TangemSearchFieldStory {
    return TangemSearchFieldStory(
        selectedShape = TangemFieldShape.RoundedCorners,
        onShapeChange = { shape ->
            updateStory { it.copy(selectedShape = shape) }
        },
    )
}

internal val tangemSearchFieldStoryFactory
    get() = storyPageFactory(StateUpdater<TangemSearchFieldStory>::build)