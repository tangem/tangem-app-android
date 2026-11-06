package com.tangem.feature.tester.presentation.storybook.page.ds.draggable

import com.tangem.core.ui.ds2.draggable.TangemDraggable
import com.tangem.feature.tester.presentation.storybook.entity.TangemDraggableStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemDraggableStory>.build(): TangemDraggableStory {
    return TangemDraggableStory(
        visualState = TangemDraggable.State.Elevated,
        onVisualStateChange = { visualState ->
            updateStory { it.copy(visualState = visualState) }
        },
    )
}

internal val tangemDraggableStoryFactory
    get() = storyPageFactory(StateUpdater<TangemDraggableStory>::build)