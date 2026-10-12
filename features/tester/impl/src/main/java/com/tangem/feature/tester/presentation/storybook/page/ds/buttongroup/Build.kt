package com.tangem.feature.tester.presentation.storybook.page.ds.buttongroup

import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.button.TangemButtonGroup
import com.tangem.feature.tester.presentation.storybook.entity.TangemButtonGroupStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemButtonGroupStory>.build(): TangemButtonGroupStory {
    return TangemButtonGroupStory(
        orientation = TangemButtonGroup.Orientation.Vertical,
        buttonCount = 2,
        size = TangemButton.Size.X12,
        isFillWidth = true,
        isLongLabel = false,
        hasIcon = false,
        onOrientationChange = { orientation ->
            updateStory { it.copy(orientation = orientation) }
        },
        onButtonCountChange = { count ->
            updateStory { it.copy(buttonCount = count) }
        },
        onSizeChange = { size ->
            updateStory { it.copy(size = size) }
        },
        onFillWidthToggle = {
            updateStory { it.copy(isFillWidth = !it.isFillWidth) }
        },
        onLongLabelToggle = {
            updateStory { it.copy(isLongLabel = !it.isLongLabel) }
        },
        onIconToggle = {
            updateStory { it.copy(hasIcon = !it.hasIcon) }
        },
    )
}

internal val tangemButtonGroupStoryFactory
    get() = storyPageFactory(StateUpdater<TangemButtonGroupStory>::build)