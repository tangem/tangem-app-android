package com.tangem.feature.tester.presentation.storybook.page.ds.modal

import com.tangem.core.ui.ds2.modal.TangemModal
import com.tangem.feature.tester.presentation.storybook.entity.TangemModalStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemModalStory>.build(): TangemModalStory {
    return TangemModalStory(
        isShown = false,
        isStackedShown = false,
        isExpanded = false,
        isContentScrollable = true,
        isTallContent = false,
        presentation = TangemModal.Presentation.Auto,
        onShownChange = { shown ->
            updateStory { it.copy(isShown = shown, isStackedShown = it.isStackedShown && shown) }
        },
        onStackedShownChange = { shown -> updateStory { it.copy(isStackedShown = shown) } },
        onExpandedToggle = { updateStory { it.copy(isExpanded = !it.isExpanded) } },
        onContentScrollableToggle = { updateStory { it.copy(isContentScrollable = !it.isContentScrollable) } },
        onTallContentToggle = { updateStory { it.copy(isTallContent = !it.isTallContent) } },
        onPresentationChange = { presentation -> updateStory { it.copy(presentation = presentation) } },
    )
}

internal val tangemModalStoryFactory
    get() = storyPageFactory(StateUpdater<TangemModalStory>::build)