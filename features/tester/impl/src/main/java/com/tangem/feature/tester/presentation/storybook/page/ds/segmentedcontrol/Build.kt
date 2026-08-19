package com.tangem.feature.tester.presentation.storybook.page.ds.segmentedcontrol

import com.tangem.feature.tester.presentation.storybook.entity.TangemSegmentedControlStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemSegmentedControlStory>.build(): TangemSegmentedControlStory {
    return TangemSegmentedControlStory(
        segmentCount = 3,
        selectedId = DEMO_SEGMENTS.first(),
        isLoading = false,
        isFillWidth = false,
        onSegmentCountChange = { count ->
            updateStory { story ->
                val visible = DEMO_SEGMENTS.take(count)
                story.copy(
                    segmentCount = count,
                    selectedId = if (story.selectedId in visible) story.selectedId else visible.first(),
                )
            }
        },
        onSegmentClick = { id -> updateStory { it.copy(selectedId = id) } },
        onLoadingToggle = { updateStory { it.copy(isLoading = !it.isLoading) } },
        onFillWidthToggle = { updateStory { it.copy(isFillWidth = !it.isFillWidth) } },
    )
}

internal val tangemSegmentedControlStoryFactory
    get() = storyPageFactory(StateUpdater<TangemSegmentedControlStory>::build)