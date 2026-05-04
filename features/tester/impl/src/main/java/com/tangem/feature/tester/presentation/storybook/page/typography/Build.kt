package com.tangem.feature.tester.presentation.storybook.page.typography

import com.tangem.feature.tester.presentation.storybook.entity.TypographyStory
import com.tangem.feature.tester.presentation.storybook.viewmodel.StateUpdater
import com.tangem.feature.tester.presentation.storybook.viewmodel.storyPageFactory

internal fun StateUpdater<TypographyStory>.build(): TypographyStory {
    return TypographyStory(
        isFontScaleDefault = false,
        onFontScaleToggle = { updateStory { it.copy(isFontScaleDefault = !it.isFontScaleDefault) } },
    )
}

internal val typographyStoryFactory
    get() = storyPageFactory(StateUpdater<TypographyStory>::build)