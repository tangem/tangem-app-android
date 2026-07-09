package com.tangem.feature.tester.presentation.storybook.page.ds.textstyle

import com.tangem.feature.tester.presentation.storybook.entity.TextStyleStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TextStyleStory>.build(): TextStyleStory {
    return TextStyleStory(
        style = TextStyleStory.Style.HeadM,
        textScale = 1f,
        onStyleChange = { style ->
            updateStory { it.copy(style = style) }
        },
        onTextScaleChange = { scale ->
            updateStory { it.copy(textScale = scale) }
        },
    )
}

internal val textStyleStoryFactory
    get() = storyPageFactory(StateUpdater<TextStyleStory>::build)