package com.tangem.feature.tester.presentation.storybook.page.ds.shimmer

import com.tangem.core.ui.ds2.shimmers.TextShimmerStyle
import com.tangem.feature.tester.presentation.storybook.entity.TangemShimmerStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TangemShimmerStory>.build(): TangemShimmerStory {
    return TangemShimmerStory(
        textStyle = TextShimmerStyle.BODY,
        radius = TangemShimmerStory.RadiusOption.R24,
        rectangleWidth = TangemShimmerStory.RectangleWidthOption.W240,
        rectangleHeight = TangemShimmerStory.RectangleHeightOption.H24,
        onTextStyleChange = { textStyle ->
            updateStory { it.copy(textStyle = textStyle) }
        },
        onRadiusChange = { radius ->
            updateStory { it.copy(radius = radius) }
        },
        onRectangleWidthChange = { width ->
            updateStory { it.copy(rectangleWidth = width) }
        },
        onRectangleHeightChange = { height ->
            updateStory { it.copy(rectangleHeight = height) }
        },
    )
}

internal val tangemShimmerStoryFactory
    get() = storyPageFactory(StateUpdater<TangemShimmerStory>::build)